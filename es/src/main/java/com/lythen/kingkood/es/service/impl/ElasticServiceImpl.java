package com.lythen.kingkood.es.service.impl;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.lythen.kingkood.core.time.LocalDateTimeHepler;
import com.lythen.kingkood.es.config.CustomElasticsearchConfig;
import com.lythen.kingkood.es.entity.BaseEsEntity;
import com.lythen.kingkood.es.params.CustomQueryField;
import com.lythen.kingkood.es.params.QueryParams;
import com.lythen.kingkood.es.service.IBaseElasticService;
import com.lythen.kingkood.es.service.IkAnalyzeService;
import lombok.Getter;
import org.elasticsearch.common.Strings;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.*;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.*;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.google.common.cache.Cache;

/**
 * 关于类的说明
 * ES操作的公共类，用于执行公共操作
 *
 * @Author lythen
 * @date 2023/6/2 14:14
 **/
public abstract class ElasticServiceImpl<T extends BaseEsEntity> implements IBaseElasticService<T> {
    @Getter
    private Class<T> tClass;
    @Autowired
    CustomElasticsearchConfig customElasticsearchConfig;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;
    @Autowired
    IkAnalyzeService ikAnalyzeService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Resource(name = "elasticSearchQueryCache")
    Cache<String, Object> elasticSearchQueryCache;

    Pattern pattern = Pattern.compile("\\d.+\\d$");

    final private String BASEKEY = "es:query:cache:";

    /**
     * 凡继承自ElasticServiceImpl的类，都必须执行这个父类的构造方法，否则后面创建索引会失败。
     */
    public ElasticServiceImpl() {
        ParameterizedType parameterizedType = getParameterizedType(getClass());
        Type[] params = parameterizedType.getActualTypeArguments();
        tClass = getClazz(params[0]);
    }


    /**
     * 根据业务ID删除已有的对象，再插入实体
     *
     * @param entity
     * @param indexPattern
     * @return
     */
    @Override
    public boolean DeleteAndInsert(T entity, String indexPattern) {

        List<T> entities = queryByYwid(entity.getYwid(), indexPattern);

        if (entities != null && !entities.isEmpty()) {
            delete(entities.stream().map(T::getId).collect(Collectors.toList()), indexPattern);
        }
        elasticsearchOperations.save(entity);
        return true;
    }

    @Override
    public int batchInsert(List<T> entityList, String indexPatern) {
        if (entityList == null || entityList.isEmpty()) {
            return 0;
        }
        System.out.printf("准备导入%d条数据。%n", entityList.size());
        IndexCoordinates indexCoordinates = elasticsearchOperations.getIndexCoordinatesFor(tClass);
        List<IndexQuery> indexQueries = convertData(entityList, indexCoordinates.getIndexName());
        Set<List<IndexedObjectInformation>> singleton = Collections.singleton(elasticsearchOperations.bulkIndex(indexQueries, tClass));
        int counter = singleton.stream().map(List::size).mapToInt(x -> x).sum();
        System.out.printf("完成导入%d条数据。%n", counter);
        indexQueries.clear();
        entityList.clear();
        // 手动刷新索引
        IndexOperations indexOps = elasticsearchOperations.indexOps(tClass);
        indexOps.refresh();
        return counter;
    }

    @Override
    public abstract int saveAll(List<T> entityList, String indexPatern);

    private List<IndexQuery> convertData(List<T> dataList, String indexName) {
        List<IndexQuery> queries = new ArrayList<>();
        for (BaseEsEntity esEntity : dataList) {
            IndexQueryBuilder indexQueryBuilder = new IndexQueryBuilder();
            IndexQuery indexQuery = indexQueryBuilder.withIndex(indexName)
                    .withObject(esEntity)
                    .withId(esEntity.getId()).withOpType(IndexQuery.OpType.INDEX)
                    .withSource(JSONObject.toJSONString(esEntity))
                    .build();
            queries.add(indexQuery);
        }
        return queries;
    }

    /**
     * 根据业务ID删除已有的对象，再插入实体
     *
     * @param entity
     * @param indexPatern
     * @return
     */
    @Override
    public boolean Insert(T entity, String indexPatern) {
        return false;
    }

    /**
     * 通过业务ID获取所有的实体
     *
     * @param ywid
     * @param indexPattern
     * @return
     */
    @Override
    public List<T> queryByYwid(String ywid, String indexPattern) {
        MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("ywid", ywid);

        IndexCoordinates indexCoordinates = getIndexCoordinates(indexPattern);
        IndexOperations indexOperations = elasticsearchOperations.indexOps(indexCoordinates);
        if (!indexOperations.exists()) {
            return new ArrayList<>();
        }
        Query query = new NativeSearchQueryBuilder()
                .withQuery(matchQueryBuilder)
                .build();
        SearchHits<T> searchHits = elasticsearchOperations.search(query, tClass, indexCoordinates);
        return searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    /**
     * 根据ID删除
     *
     * @param id
     * @param indexPattern
     */
    @Override
    public void delete(String id, String indexPattern) {
        IndexCoordinates indexCoordinates = getIndexCoordinates(indexPattern);
        elasticsearchOperations.delete(id, indexCoordinates);
    }

    @Override
    public IndexCoordinates getIndexCoordinates(String indexPattern) {
        IndexCoordinates indexCoordinates = null;
        String tempIndexPattern = indexPattern;
        if (Strings.isNullOrEmpty(indexPattern)) {
            indexCoordinates = elasticsearchOperations.getIndexCoordinatesFor(tClass);

            String indexName = indexCoordinates.getIndexName();
            Matcher matcher = pattern.matcher(indexName);
            tempIndexPattern = matcher.replaceAll("*");
        }
        indexCoordinates = IndexCoordinates.of(tempIndexPattern);
        return indexCoordinates;
    }

    /**
     * 根据ID批量删除
     *
     * @param idList
     * @param indexPattern
     */
    @Override
    public void delete(List<String> idList, String indexPattern) {
        IndexCoordinates indexCoordinates = getIndexCoordinates(indexPattern);
        IdsQueryBuilder idsQueryBuilder = new IdsQueryBuilder();
        idsQueryBuilder.addIds(idList.toArray(new String[0]));
        Query query = new NativeSearchQueryBuilder()
                .withQuery(idsQueryBuilder)
                .build();
        elasticsearchOperations.delete(query, tClass, indexCoordinates);
    }

    /**
     * 每个条件都是模糊查询，而且，只要有一个符合即可
     *
     * @param queryMap
     * @param indexPattern
     * @return
     */
    @Override
    public List<T> queryByAllConditionsCanByTenable(Map<String, String> queryMap, String indexPattern) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        queryMap.forEach((k, v) -> {
            MatchPhraseQueryBuilder matchPhraseQueryBuilder = QueryBuilders.matchPhraseQuery(k, v);
            boolQueryBuilder.should(matchPhraseQueryBuilder);
        });
        Query query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();

        IndexCoordinates indexCoordinates = getIndexCoordinates(indexPattern);
        SearchHits<T> searchHits = elasticsearchOperations.search(query, tClass, indexCoordinates);
        return searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    /**
     * 条件查询，每个条件都是模糊查询，但是每个条件都要符合才可以
     *
     * @param queryMap
     * @param indexPattern
     * @return
     */
    @Override
    public List<T> queryByAllConditionsMustByTenable(Map<String, String> queryMap, String indexPattern) {
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        queryMap.forEach((k, v) -> {
            MatchPhraseQueryBuilder matchPhraseQueryBuilder = QueryBuilders.matchPhraseQuery(k, v);
            boolQueryBuilder.must(matchPhraseQueryBuilder);
        });
        Query query = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .build();

        IndexCoordinates indexCoordinates = getIndexCoordinates(indexPattern);

        SearchHits<T> searchHits = elasticsearchOperations.search(query, tClass, indexCoordinates);
        return searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
    }

    /**
     * 根据参数查询ES库
     *
     * @param queryParams
     * @return
     */
    @Override
    public List<T> list(QueryParams queryParams, QueryBuilder... queryBuilders) {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        List<FieldSortBuilder> fieldSortBuilderList = new ArrayList<>();
        String indexName = queryParams.getIndexName();
        if(Strings.isNullOrEmpty(indexName)){
            indexName = getIndexCoordinates(null).getIndexName();
        }
        // 查询参数构建
        if (StrUtil.isNotBlank(queryParams.getKeyword())) {
            Set<String> keywordList = ikAnalyzeService.getIkAnalyzeSearchTerms(indexName,"ik_smart", queryParams.getKeyword());
            for (String s : keywordList) {
                boolQueryBuilder.filter(QueryBuilders.termQuery(queryParams.getKeywordField(), s));
            }
        }

        if (queryParams.getStartDate() != null) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("ywblStartTime")
                    .gte(LocalDateTimeHepler.TransToUTCString(queryParams.getStartDate(), null));
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        if (queryParams.getEndDate() != null) {
            RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("ywblEndTime")
                    .lte(LocalDateTimeHepler.TransToUTCString(queryParams.getEndDate(), null));
            boolQueryBuilder.filter(rangeQueryBuilder);
        }
        if (!Strings.isNullOrEmpty(queryParams.getId())) {
            boolQueryBuilder.filter(QueryBuilders.idsQuery().addIds(queryParams.getId()));
        }
        if (!Strings.isNullOrEmpty(queryParams.getYwid())) {
            boolQueryBuilder.filter(QueryBuilders.termQuery("ywid", queryParams.getYwid()));
        }
        // 设置排序
        if (queryParams.getSort() != null) {
            queryParams.getSort().forEach((k, v) -> {
                FieldSortBuilder sortBuilder;
                if ("DESC".equals(v)) {
                    sortBuilder = SortBuilders.fieldSort(k).order(SortOrder.DESC);
                } else {
                    sortBuilder = SortBuilders.fieldSort(k).order(SortOrder.ASC);
                }
                fieldSortBuilderList.add(sortBuilder);
            });
        }
        // 设置输出指定的字段，或者设置排除输出指定的字段，可以两个都不设置，但只会其中一个配置生效。
        if (queryParams.getIncludes() != null && queryParams.getIncludes().length > 0) {
            nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(queryParams.getIncludes(), null));
        } else if (queryParams.getExcludes() != null && queryParams.getExcludes().length > 0) {
            nativeSearchQueryBuilder.withSourceFilter(new FetchSourceFilter(null, queryParams.getExcludes()));
        }
        if (!fieldSortBuilderList.isEmpty()) {
            nativeSearchQueryBuilder.withSorts(fieldSortBuilderList.toArray(new SortBuilder[0]));
        }

        //查看是否有search_after参数
        List<Object> sortObjects = this.getSortValues(queryParams.getQueryId(), queryParams.getPageIndex());
        // 分页不为0，但是没有排序信息，则报错
        if ((sortObjects == null || sortObjects.isEmpty()) && queryParams.getPageIndex() > 0) {
            throw new RuntimeException("查询失败，查询已失效，请重新发起查询。");
        }
        PageRequest pageRequest = null;
        int pageindex = queryParams.getPageIndex();
        if (sortObjects != null && !sortObjects.isEmpty()) {
            nativeSearchQueryBuilder.withSearchAfter(sortObjects);
            // 当有search_after的时候，from必须设置为0，但是不能直接设置 queryParams.setPageIndex(0)，因为这个页号还是要用到的。
            pageindex = 0;
        }
        // 设置分页
        if (queryParams.getPageSize() > 0) {
            pageRequest = PageRequest.of(pageindex, queryParams.getPageSize());
            nativeSearchQueryBuilder.withPageable(pageRequest);
        }
        // 使用传入的自定义builder
        if (queryBuilders != null) {
            for (QueryBuilder queryBuilder : queryBuilders
            ) {
                nativeSearchQueryBuilder.withFilter(queryBuilder);
            }
        }
        // 自定义的查询
        if (queryParams.getQueryFieldsList() != null && !queryParams.getQueryFieldsList().isEmpty()) {
            queryParams.getQueryFieldsList().forEach(queryField -> boolQueryBuilder.filter(this.getBuilder(queryField)));
        }
        // 高亮
        if (queryParams.getHighlightFields() != null && !queryParams.getHighlightFields().isEmpty()) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            for (String highlightField : queryParams.getHighlightFields()) {
                highlightBuilder.field(highlightField);
            }
            highlightBuilder.preTags(String.format("<span style='color:%s'>", customElasticsearchConfig.getHighlightColor())).postTags("</span>");
            nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);
        }
        // 构建查询实体
        Query query = nativeSearchQueryBuilder
                .withQuery(boolQueryBuilder)
                .build();
        IndexCoordinates indexCoordinates = getIndexCoordinates(queryParams.getIndexName());

        SearchHits<T> searchHits = elasticsearchOperations.search(query, tClass, indexCoordinates);
        if (searchHits.getTotalHits() == 0) {
            return new ArrayList<>();
        }
        this.setSortValues(queryParams.getQueryId(), queryParams.getPageIndex(), searchHits);

        List<T> collect = searchHits.get().map(SearchHit::getContent).collect(Collectors.toList());
        collect.forEach(t ->
            searchHits.get().forEach(searchHit -> {
                if (searchHit.getContent().getId().equals(t.getId())) {
                    Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
                    t.setHighlightFields(highlightFields);
                    return;
                }
            })
        );
        return collect;
    }
    /**
     * 根据参数查询ES库
     * 直接调用match方法
     * @param queryParams
     * @return
     */
    @Override
    public List<T> listMatch(QueryParams queryParams, QueryBuilder... queryBuilders) {
        // 查询参数构建
        if (StrUtil.isNotBlank(queryParams.getKeyword())) {
            MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery(queryParams.getKeywordField(), queryParams.getKeyword()).operator(Operator.valueOf("AND"));
            queryParams.setKeyword(null);
            return list(queryParams,matchQueryBuilder);
        }else {
            return list(queryParams,queryBuilders);
        }
    }
    /**
     * 递归获取 ParameterizedType
     * （对于一个实现类，它有可能是父类的父类才是 AbstractService）
     */
    private ParameterizedType getParameterizedType(Type type) {
        if (type instanceof ParameterizedType) {
            return (ParameterizedType) type;
        } else if (type instanceof Class) {
            return getParameterizedType((((Class<?>) type).getGenericSuperclass()));
        } else {
            return null;
        }
    }

    /**
     * 根据请求ID和页码，获取之前请求时的排序值，主要用于“上一页”的操作。
     *
     * @param queryId
     * @param pageIndex
     * @return
     */
    private List<Object> getSortValues(String queryId, int pageIndex) {

        String key = BASEKEY + queryId + "_" + pageIndex;
        if (pageIndex == 0 || Strings.isNullOrEmpty(queryId)) {
            return new ArrayList<>();
        }
        // 这个数据是必须缓存的,如果没有redis,就存在本地缓存里
        if (redisTemplate == null) {
            Object object = elasticSearchQueryCache.getIfPresent(key);
            if (object != null) {
                return (List<Object>) object;
            }
        }
        // 在保存的时候,已经全部转换成了string了,但为了ES的排序速度,统一使用long类型,所以要转换一下.
        Set<String> listCache = redisTemplate.opsForSet().members(key);
        if (listCache != null && !listCache.isEmpty()) {
            return listCache.stream().map(Long::parseLong).collect(Collectors.toList());
        }
        return null;
    }

    /**
     * 设置排序值，用于下一页查询。
     * 如果当前为第0页，则这个是第0页的排序值，在查询第1页时才需要。
     *
     * @param queryId 查询的标识
     * @param pageIndex 页码，用于计算排序值的标识
     * @param searchHits 搜索结果集
     */
    private void setSortValues(String queryId, int pageIndex, SearchHits searchHits) {
        // 排序的排序值,是给下一页用的,如果当前为第0页,那这个是第0页的排序值,在查询第1页时才需要
        pageIndex = pageIndex + 1;
        String key = BASEKEY + queryId + "_" + pageIndex;

        // 获取搜索结果集的大小
        int size = searchHits.getSearchHits().size();

        // 如果搜索结果集为空，则直接返回
        if (size == 0) {
            return;
        }

        // 获取最后一个搜索结果的排序值列表
        List<Object> sortObjects = ((SearchHit) (searchHits).getSearchHits().get(size - 1)).getSortValues();

        // 如果排序值列表为空，则直接返回
        if (sortObjects.isEmpty()) {
            return;
        }

        // 如果redisTemplate为空，则将排序值列表存入缓存
        if (redisTemplate == null) {
            elasticSearchQueryCache.put(key, sortObjects);
        } else {
            // 将排序值列表转换成字符串数组
            String[] cacheList = sortObjects.stream().map(Convert::toStr).collect(Collectors.toList()).toArray(new String[sortObjects.size()]);

            // 将排序值添加到redis中的set集合中
            redisTemplate.opsForSet().add(key, cacheList);

            // 设置redis中排序值键的过期时间为1天
            redisTemplate.expire(key, Duration.ofDays(1));
        }
    }

    /**
     * 递归获取 Class
     * （对于一个实现类，它有可能是父类的父类才是 AbstractService）
     */
    private Class getClazz(Type type) {
        if (type instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) type).getRawType();
            return getClazz(rawType);
        } else {
            return (Class) type;
        }
    }

    /**
     * 根据自定义参数构建查询
     *
     * @param queryFields
     * @return
     */
    private QueryBuilder getBuilder(CustomQueryField queryFields) {
        switch (queryFields.getQueryType()) {
            case MATCH:
                return QueryBuilders.matchQuery(queryFields.getFieldName(), queryFields.getFieldValue());
            case MATCH_PHRASE:
                return QueryBuilders.matchPhraseQuery(queryFields.getFieldName(), queryFields.getFieldValue());
            case MATCH_ALL:
                return QueryBuilders.matchAllQuery();
            case MATCH_PHRASE_PREFIX:
                return QueryBuilders.matchPhrasePrefixQuery(queryFields.getFieldName(), queryFields.getFieldValue());
            case TERM:
                return QueryBuilders.termQuery(queryFields.getFieldName(), queryFields.getFieldValue());
        }
        throw new RuntimeException("查询类型不支持");
    }
}
