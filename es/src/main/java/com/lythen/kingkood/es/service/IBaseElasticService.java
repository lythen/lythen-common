package com.lythen.kingkood.es.service;

import com.lythen.kingkood.es.params.QueryParams;
import org.elasticsearch.index.query.QueryBuilder;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 关于类的说明
 *
 * @Author lythen
 * @date 2023/6/2 17:43
 **/
public interface IBaseElasticService<T> {
    /**
     * 使用bulkIndex进行批量插入
     *
     * @param entityList
     * @param indexPatern
     * @return
     */
    int batchInsert(List<T> entityList, String indexPatern);

    /**
     * 使用CrudRepository.saveAll进行批量插入
     *
     * @param entityList
     * @param indexPatern
     * @return
     */
    int saveAll(List<T> entityList, String indexPatern);

    /**
     * 根据业务ID删除已有的对象，再插入实体
     *
     * @param indexPatern
     * @param entity
     * @return
     */
    boolean Insert(T entity, String indexPatern);

    /**
     * 根据业务ID删除已有的对象，再插入实体
     *
     * @param indexPatern
     * @param entity
     * @return
     */
    boolean DeleteAndInsert(T entity, String indexPatern);

    /**
     * 通过业务ID获取所有的实体
     *
     * @param ywid
     * @param indexPatern
     * @return
     */
    List<T> queryByYwid(String ywid, String indexPatern);

    /**
     * 条件查询，每个条件都是模糊查询，但是每个条件都要符合才可以
     *
     * @param indexPatern
     * @param queryMap
     * @return
     */
    List<T> queryByAllConditionsMustByTenable(Map<String, String> queryMap, String indexPatern);

    /**
     * 每个条件都是模糊查询，而且，只要有一个符合即可
     *
     * @param indexPatern
     * @param queryMap
     * @return
     */
    List<T> queryByAllConditionsCanByTenable(Map<String, String> queryMap, String indexPatern);


    /**
     * 根据ID删除
     *
     * @param id
     */
    void delete(String id, String indexPatern);

    /**
     * 根据ID批量删除
     *
     * @param id
     */
    void delete(List<String> id, String indexPatern);

    /**
     * 如果传入的是空，则表示查全部，需要处理从实体取得的
     *
     * @param indexPatern
     * @return
     */
    IndexCoordinates getIndexCoordinates(String indexPatern) throws IOException;

    /**
     * 列表查询，支持分页
     *
     * @param queryParams
     * @param queryBuilder
     * @return
     */
    List<T> list(QueryParams queryParams, QueryBuilder... queryBuilder);

    /**
     * 列表查询，支持分页,直接调用match方法
     *
     * @param queryParams
     * @param queryBuilder
     * @return
     */
    List<T> listMatch(QueryParams queryParams, QueryBuilder... queryBuilder);
}
