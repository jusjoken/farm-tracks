package ca.jusjoken.data.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import ca.jusjoken.data.ColumnSort;
import ca.jusjoken.data.Utility.StockSaleStatus;
import ca.jusjoken.data.entity.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class StockRepositoryImpl implements StockRepositoryCustom {

    private static final List<String> TERMINAL_STATUSES = List.of(
            "inactive", "archived", "butchered", "culled", "died", "sold");

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<Stock> findAllForGrid(StockGridQuery query, Pageable pageable) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Stock> criteria = builder.createQuery(Stock.class);
        Root<Stock> root = criteria.from(Stock.class);

        criteria.select(root);
        criteria.where(buildPredicate(query, builder, root));
        criteria.orderBy(buildOrders(query, builder, root));

        TypedQuery<Stock> typedQuery = entityManager.createQuery(criteria);
        if (pageable != null && pageable.isPaged()) {
            typedQuery.setFirstResult((int) pageable.getOffset());
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        return typedQuery.getResultList();
    }

    @Override
    public List<Stock> findAllForGrid(StockGridQuery query) {
        return findAllForGrid(query, Pageable.unpaged());
    }

    @Override
    public long countForGrid(StockGridQuery query) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<Stock> root = criteria.from(Stock.class);

        criteria.select(builder.count(root));
        criteria.where(buildPredicate(query, builder, root));

        return entityManager.createQuery(criteria).getSingleResult();
    }

    @Override
    public double sumStockValueForGrid(StockGridQuery query) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> criteria = builder.createQuery(Double.class);
        Root<Stock> root = criteria.from(Stock.class);

        criteria.select(builder.coalesce(builder.sum(root.get("stockValue")), 0.0));
        criteria.where(buildPredicate(query, builder, root));

        Double result = entityManager.createQuery(criteria).getSingleResult();
        return result == null ? 0.0 : result;
    }

    private Predicate buildPredicate(StockGridQuery query, CriteriaBuilder builder, Root<Stock> root) {
        List<Predicate> predicates = new ArrayList<>();

        if (query == null) {
            return builder.conjunction();
        }

        if (query.stockTypeId() != null) {
            predicates.add(builder.equal(root.get("stockType").get("id"), query.stockTypeId()));
        }

        if (query.breeder() != null) {
            predicates.add(builder.equal(root.get("breeder"), query.breeder()));
        }

        if (query.namePrefix() != null && !query.namePrefix().isBlank()) {
            Expression<String> loweredName = builder.lower(builder.coalesce(root.get("name"), ""));
            predicates.add(builder.like(loweredName, query.namePrefix().trim().toLowerCase() + "%"));
        }

        String statusName = query.stockStatusName();
        if (statusName != null && !statusName.isBlank()) {
            Predicate activePredicate = buildActivePredicate(builder, root);
            Predicate externalPredicate = buildExternalPredicate(builder, root);
            Predicate nonExternalPredicate = builder.not(externalPredicate);

            switch (statusName.trim().toLowerCase()) {
                case "active" -> {
                    predicates.add(query.includeExternal()
                            ? builder.or(activePredicate, externalPredicate)
                            : builder.and(activePredicate, nonExternalPredicate));
                }
                case "inactive" -> {
                    Predicate inactivePredicate = builder.not(activePredicate);
                    predicates.add(query.includeExternal()
                            ? builder.or(inactivePredicate, externalPredicate)
                            : builder.and(inactivePredicate, nonExternalPredicate));
                }
                case "all" -> {
                    if (!query.includeExternal()) {
                        predicates.add(nonExternalPredicate);
                    }
                }
                default -> {
                    predicates.add(builder.equal(
                            builder.lower(builder.coalesce(root.get("status"), "")),
                            statusName.trim().toLowerCase()));
                    if (!query.includeExternal()) {
                        predicates.add(nonExternalPredicate);
                    }
                }
            }
        } else if (!query.includeExternal()) {
            predicates.add(builder.not(buildExternalPredicate(builder, root)));
        }

        return predicates.isEmpty()
                ? builder.conjunction()
                : builder.and(predicates.toArray(Predicate[]::new));
    }

    private Predicate buildActivePredicate(CriteriaBuilder builder, Root<Stock> root) {
        Expression<String> normalizedStatus = builder.lower(
            builder.trim(builder.coalesce(root.get("status"), "")));

        Predicate saleNotSold = builder.or(
                builder.isNull(root.get("saleStatus")),
                builder.notEqual(root.get("saleStatus"), StockSaleStatus.SOLD));
        Predicate statusBlank = builder.equal(normalizedStatus, "");
        Predicate statusNotTerminal = builder.not(normalizedStatus.in(TERMINAL_STATUSES));

        return builder.and(saleNotSold, builder.or(statusBlank, statusNotTerminal));
    }

    private Predicate buildExternalPredicate(CriteriaBuilder builder, Root<Stock> root) {
        return builder.isTrue(builder.coalesce(root.get("external"), Boolean.FALSE));
    }

    private List<Order> buildOrders(StockGridQuery query, CriteriaBuilder builder, Root<Stock> root) {
        List<ColumnSort> configuredSortOrders = query == null ? List.of() : query.safeSortOrders();
        List<Order> orders = new ArrayList<>();

        List<ColumnSort> effectiveSortOrders = configuredSortOrders.isEmpty()
                ? List.of(new ColumnSort("name", Sort.Direction.ASC))
                : configuredSortOrders;

        ColumnSort primarySort = effectiveSortOrders.get(0);
        if (primarySort != null && "name".equals(primarySort.getColumnName())) {
                Expression<String> normalizedName = builder.trim(builder.coalesce(root.get("name"), ""));
            Expression<Integer> blankLast = builder.<Integer>selectCase()
                    .when(builder.equal(normalizedName, ""), 1)
                    .otherwise(0);
            orders.add(builder.asc(blankLast));
        }

        for (ColumnSort sortOrder : effectiveSortOrders) {
            if (sortOrder == null || sortOrder.getColumnName() == null || sortOrder.getColumnName().isBlank()) {
                continue;
            }
            Expression<?> expression = root.get(sortOrder.getColumnName());
            orders.add(sortOrder.getColumnSortDirection() == Sort.Direction.DESC
                    ? builder.desc(expression)
                    : builder.asc(expression));
        }

        return orders;
    }
}