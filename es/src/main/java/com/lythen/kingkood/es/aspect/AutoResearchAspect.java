package com.lythen.kingkood.es.aspect;

import com.lythen.kingkood.es.config.CustomElasticsearchConfig;
import com.lythen.kingkood.es.params.QueryParams;
import com.lythen.kingkood.es.service.IBaseElasticService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.elasticsearch.index.query.QueryBuilder;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/11/9 17:41
 **/
@Aspect
@Component("autoResearchAspect")
@Slf4j
public class AutoResearchAspect {
    final private String LOCKKEY = "es:analyze:lock:";
    final private String BASEKEY = "es:query:cache:";

    @Autowired
    RedissonClient redissonClient;

    @Autowired
    CustomElasticsearchConfig customElasticsearchConfig;

    @Resource(name = "esThreadPoolExecutor")
    ThreadPoolExecutor threadPoolExecutor;
    @Autowired
    private StringRedisTemplate redisTemplate;

    @Pointcut("execution(* *..list(..)) && this(com.lythen.kingkood.es.service.IBaseElasticService)")
    public void listPointCut() {

    }

    /**
     * 实现了自动查询后，就要考虑重复查询的问题
     * 例如在自动查询的时候，用户也在查询，这会导致同时查同一页的数据
     * 所以这里要加锁
     * 但其实还是会有一个问题，就是如果这里的锁先进，也就是用户操作早于自动查询
     * 就会报错，但是这个无所谓了
     *
     * @param point
     * @return
     */
    @Around("listPointCut()")
    public Object search(ProceedingJoinPoint point) throws InterruptedException {
        Object[] args = point.getArgs();
        QueryParams queryParams =  (QueryParams) args[0];
        String lockKey = String.format("%s%s_%d", LOCKKEY, queryParams.getQueryId(), queryParams.getPageIndex());
        Lock lock =  redissonClient.getLock(lockKey);

        if (lock.tryLock(30, TimeUnit.SECONDS)) {
            try {
                return point.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            } finally {
                lock.unlock();
            }
        }
        return null;
    }
    @AfterReturning(value = "listPointCut()", returning = "result")
    public void afterResearch(JoinPoint point, Object result) {
        if(customElasticsearchConfig.getAutoSearchClass()==null || customElasticsearchConfig.getAutoSearchClass().length==0){
            return;
        }
        // 查询返回之后执行，将后面的查询也查了，获取排序结果
        Object target = point.getTarget();
        Class targetClass = target.getClass();
        if(!Arrays.stream(customElasticsearchConfig.getAutoSearchClass()).allMatch(c->c.isAssignableFrom(targetClass))){
            return;
        }
        // 先要取到使用的方法，后面要用的是同名的另一个方法。
        Signature signature = point.getSignature();
        String oriMethodName = signature.getName();
        Object[] args = point.getArgs();
        try {
            // 看下有没有同名的方法，并且带了IndexCoordinates这个参数的，如果有就调用，没有就算了。
            Method method = ((IBaseElasticService) target).getClass().getMethod(oriMethodName, QueryParams.class, QueryBuilder[].class);
            if (method != null) {

                // 通过反射执行
                autoSearch(method, target, (QueryParams) args[0], (QueryBuilder[]) args[1]);
            }
        } catch (NoSuchMethodException | CloneNotSupportedException ex) {

        }
    }

    /**
     * 根据当前页码和配置的最大页码，自动查询剩余的
     * 如果已有缓存，则直接返回
     *
     * @param queryParams
     * @param queryBuilder
     */
    private void autoSearch(Method method, Object target, QueryParams queryParams, QueryBuilder... queryBuilder) throws CloneNotSupportedException {
        int beginPage = queryParams.getPageIndex() + 1;
        int max = beginPage + customElasticsearchConfig.getAutoSearchPageCount();
        // 配置校验
        if (!customElasticsearchConfig.isAutoSearch()) {
            return;
        }
        if (customElasticsearchConfig.getAutoSearchPageCount() == 0) {
            return;
        }

        threadPoolExecutor.execute(() -> {
            // 必须按顺序执行
            for (int i = beginPage; i < max; i++) {
                queryParams.setPageIndex(i);
                // 要看它下一页缓存存不存在，执行当前页的查询，是为了生成下一页的排序值
                String cacheKey = String.format("%s%s_%d", BASEKEY, queryParams.getQueryId(), queryParams.getPageIndex() + 1);
                if (redisTemplate.hasKey(cacheKey)) {
                    continue;
                }
                String lockKey = String.format("%s%s_%d", LOCKKEY, queryParams.getQueryId(), queryParams.getPageIndex());
                Lock lock = redissonClient.getLock(lockKey);
                // 到了这里，说明即不是第一页，也没有缓存排序值，那就需要查询生成排序值
                try {
                    if (lock.tryLock(30, TimeUnit.SECONDS)) {
                        try {
                            if (redisTemplate.hasKey(cacheKey)) {
                                continue;
                            }
                            // 通过反射执行
                            method.invoke(target, queryParams, queryBuilder);

                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            lock.unlock();
                        }
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });

    }
}
