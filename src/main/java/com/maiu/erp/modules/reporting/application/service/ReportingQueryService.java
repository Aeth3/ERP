package com.maiu.erp.modules.reporting.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.project.application.dto.ProjectCostSummaryDto;
import com.maiu.erp.modules.project.application.service.ProjectCostQueryService;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;
import com.maiu.erp.modules.reporting.application.dto.InventorySummaryDto;
import com.maiu.erp.modules.reporting.application.dto.MovementSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.MovementTypeCountDto;
import com.maiu.erp.modules.reporting.application.dto.ProjectRiskItemDto;
import com.maiu.erp.modules.reporting.application.dto.ProjectSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.ReportMovementItemDto;
import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportSummaryDto;

@Service
public class ReportingQueryService {
    private final InventoryService inventoryService;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProjectCostQueryService projectCostQueryService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final PurchaseReturnRepository purchaseReturnRepository;
    private final SupplierRepository supplierRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesReturnRepository salesReturnRepository;
    private final CustomerRepository customerRepository;

    public ReportingQueryService(
            InventoryService inventoryService,
            ProjectRepository projectRepository,
            ProductRepository productRepository,
            WarehouseRepository warehouseRepository,
            ProjectCostQueryService projectCostQueryService,
            PurchaseOrderRepository purchaseOrderRepository,
            PurchaseOrderItemRepository purchaseOrderItemRepository,
            PurchaseReturnRepository purchaseReturnRepository,
            SupplierRepository supplierRepository,
            SalesOrderRepository salesOrderRepository,
            SalesOrderItemRepository salesOrderItemRepository,
            SalesReturnRepository salesReturnRepository,
            CustomerRepository customerRepository) {
        this.inventoryService = inventoryService;
        this.projectRepository = projectRepository;
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.projectCostQueryService = projectCostQueryService;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.purchaseReturnRepository = purchaseReturnRepository;
        this.supplierRepository = supplierRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.salesOrderItemRepository = salesOrderItemRepository;
        this.salesReturnRepository = salesReturnRepository;
        this.customerRepository = customerRepository;
    }

    public ReportingOverviewDto getOverview(LocalDate startDate, LocalDate endDate) {
        List<InventoryStock> stocks = inventoryService.getAllStocks();
        List<Project> projects = projectRepository.findAll();
        List<Product> products = productRepository.findAll();
        List<Warehouse> warehouses = warehouseRepository.findAll();
        List<StockMovement> movements = inventoryService.getStockMovements(
                null,
                null,
                null,
                null,
                null,
                null,
                startDate,
                endDate);

        Map<UUID, Product> productsById = toMapById(products, Product::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouses, Warehouse::getId);
        Map<UUID, Project> projectsById = toMapById(projects, Project::getId);

        return new ReportingOverviewDto(
                buildInventorySummary(stocks, products.size(), warehouses.size()),
                buildMovementSummary(startDate, endDate, movements, productsById, warehousesById, projectsById),
                buildProjectSummary(projects));
    }

    public PurchaseOrderReportSummaryDto getPurchaseOrderReport(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID projectId,
            UUID supplierId) {
        Map<UUID, Project> projectsById = toMapById(projectRepository.findAll(), Project::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Supplier> suppliersById = toMapById(supplierRepository.findAll(), Supplier::getId);

        List<PurchaseOrderReportItemDto> orders = purchaseOrderRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(order -> isWithinDateRange(order.getOrderDate(), startDate, endDate))
                .filter(order -> hasMatchingStatus(order.getStatus(), status))
                .filter(order -> projectId == null || projectId.equals(order.getProjectId()))
                .filter(order -> supplierId == null || supplierId.equals(order.getSupplierId()))
                .sorted(Comparator
                        .comparing(PurchaseOrder::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(PurchaseOrder::getPoNumber, Comparator.nullsLast(String::compareTo)))
                .map(order -> toPurchaseOrderReportItem(order, suppliersById, projectsById, warehousesById))
                .toList();

        BigDecimal totalAmount = orders.stream()
                .map(PurchaseOrderReportItemDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PurchaseOrderReportSummaryDto(
                startDate,
                endDate,
                orders.size(),
                totalAmount,
                orders);
    }

    public SalesOrderReportSummaryDto getSalesOrderReport(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID customerId,
            UUID warehouseId) {
        Map<UUID, Customer> customersById = toMapById(customerRepository.findAll(), Customer::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);

        List<SalesOrderReportItemDto> orders = salesOrderRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(order -> isWithinDateRange(order.getOrderDate(), startDate, endDate))
                .filter(order -> hasMatchingStatus(order.getStatus(), status))
                .filter(order -> customerId == null || customerId.equals(order.getCustomerId()))
                .filter(order -> warehouseId == null || warehouseId.equals(order.getConfirmedWarehouseId()))
                .sorted(Comparator
                        .comparing(SalesOrder::getOrderDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed()
                        .thenComparing(SalesOrder::getSoNumber, Comparator.nullsLast(String::compareTo)))
                .map(order -> toSalesOrderReportItem(order, customersById, warehousesById))
                .toList();

        BigDecimal totalAmount = orders.stream()
                .map(SalesOrderReportItemDto::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new SalesOrderReportSummaryDto(
                startDate,
                endDate,
                orders.size(),
                totalAmount,
                orders);
    }

    private InventorySummaryDto buildInventorySummary(
            List<InventoryStock> stocks,
            int productCount,
            int warehouseCount) {
        BigDecimal totalOnHand = stocks.stream()
                .map(InventoryStock::getQuantityOnHand)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalReserved = stocks.stream()
                .map(InventoryStock::getReservedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAvailable = totalOnHand.subtract(totalReserved);
        int stockedWarehouseCount = (int) stocks.stream()
                .map(InventoryStock::getWarehouseId)
                .distinct()
                .count();

        return new InventorySummaryDto(
                totalOnHand,
                totalReserved,
                totalAvailable,
                stocks.size(),
                productCount,
                warehouseCount,
                stockedWarehouseCount);
    }

    private MovementSummaryDto buildMovementSummary(
            LocalDate startDate,
            LocalDate endDate,
            List<StockMovement> movements,
            Map<UUID, Product> productsById,
            Map<UUID, Warehouse> warehousesById,
            Map<UUID, Project> projectsById) {
        List<MovementTypeCountDto> topMovementTypes = movements.stream()
                .collect(Collectors.groupingBy(movement -> movement.getMovementType().name(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> new MovementTypeCountDto(entry.getKey(), entry.getValue().intValue()))
                .toList();

        List<ReportMovementItemDto> recentMovements = movements.stream()
                .sorted(Comparator.comparing(StockMovement::getMovementDate).reversed())
                .limit(10)
                .map(movement -> toMovementItem(movement, productsById, warehousesById, projectsById))
                .toList();

        return new MovementSummaryDto(
                startDate,
                endDate,
                movements.size(),
                topMovementTypes,
                recentMovements);
    }

    private ProjectSummaryDto buildProjectSummary(List<Project> projects) {
        List<ProjectCostSummaryDto> summaries = projects.stream()
                .filter(Objects::nonNull)
                .filter(project -> project.getId() != null)
                .map(project -> projectCostQueryService.getProjectCostSummary(project.getId()))
                .toList();

        int activeProjectCount = (int) projects.stream()
                .filter(Objects::nonNull)
                .filter(project -> project.getStatus() == ProjectStatus.ACTIVE)
                .count();
        int overBudgetProjectCount = (int) summaries.stream()
                .filter(summary -> summary.getActualVariance() != null)
                .filter(summary -> summary.getActualVariance().compareTo(BigDecimal.ZERO) < 0)
                .count();
        BigDecimal totalCommittedCost = summaries.stream()
                .map(ProjectCostSummaryDto::getCommittedCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalActualCost = summaries.stream()
                .map(ProjectCostSummaryDto::getActualCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<UUID, Project> projectsById = toMapById(projects, Project::getId);

        List<ProjectRiskItemDto> budgetRiskProjects = summaries.stream()
                .filter(summary -> summary.getBudgetBasisAmount() != null)
                .sorted(Comparator.comparing(ProjectCostSummaryDto::getActualVariance, Comparator.nullsLast(BigDecimal::compareTo)))
                .limit(5)
                .map(summary -> {
                    Project project = projectsById.get(summary.getProjectId());
                    return new ProjectRiskItemDto(
                            summary.getProjectId(),
                            project == null ? null : project.getProjectCode(),
                            summary.getProjectName(),
                            project == null ? null : project.getStatus(),
                            summary.getBudgetBasisAmount(),
                            summary.getCommittedCost(),
                            summary.getActualCost(),
                            summary.getActualVariance());
                })
                .toList();

        return new ProjectSummaryDto(
                projects.size(),
                activeProjectCount,
                overBudgetProjectCount,
                totalCommittedCost,
                totalActualCost,
                budgetRiskProjects);
    }

    private ReportMovementItemDto toMovementItem(
            StockMovement movement,
            Map<UUID, Product> productsById,
            Map<UUID, Warehouse> warehousesById,
            Map<UUID, Project> projectsById) {
        Product product = productsById.get(movement.getProductId());
        Warehouse warehouse = warehousesById.get(movement.getWarehouseId());
        Project project = movement.getProjectId() == null ? null : projectsById.get(movement.getProjectId());

        return new ReportMovementItemDto(
                movement.getId(),
                movement.getMovementType().name(),
                movement.getQuantity(),
                movement.getUnitCost(),
                movement.getReferenceType(),
                movement.getReferenceId(),
                movement.getRemarks(),
                movement.getPerformedBy(),
                movement.getMovementDate(),
                movement.getProductId(),
                product == null ? null : product.getName(),
                product == null ? null : product.getSku(),
                movement.getWarehouseId(),
                warehouse == null ? null : warehouse.getName(),
                warehouse == null ? null : warehouse.getCode(),
                movement.getProjectId(),
                project == null ? null : project.getProjectCode(),
                project == null ? null : project.getProjectName());
    }

    private PurchaseOrderReportItemDto toPurchaseOrderReportItem(
            PurchaseOrder order,
            Map<UUID, Supplier> suppliersById,
            Map<UUID, Project> projectsById,
            Map<UUID, Warehouse> warehousesById) {
        Supplier supplier = suppliersById.get(order.getSupplierId());
        Project project = order.getProjectId() == null ? null : projectsById.get(order.getProjectId());
        Warehouse warehouse = order.getReceivedWarehouseId() == null ? null : warehousesById.get(order.getReceivedWarehouseId());
        int itemCount = purchaseOrderItemRepository.findByPurchaseOrderId(order.getId()).size();
        int returnCount = (int) purchaseReturnRepository.findByPurchaseOrderId(order.getId()).stream().map(PurchaseReturn::getId).count();

        return new PurchaseOrderReportItemDto(
                order.getId(),
                order.getPoNumber(),
                order.getStatus().name(),
                order.getOrderDate(),
                order.getExpectedDate(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getSupplierId(),
                supplier == null ? null : supplier.getCode(),
                supplier == null ? null : supplier.getName(),
                order.getProjectId(),
                project == null ? null : project.getProjectCode(),
                project == null ? null : project.getProjectName(),
                order.getReceivedWarehouseId(),
                warehouse == null ? null : warehouse.getCode(),
                warehouse == null ? null : warehouse.getName(),
                itemCount,
                returnCount);
    }

    private SalesOrderReportItemDto toSalesOrderReportItem(
            SalesOrder order,
            Map<UUID, Customer> customersById,
            Map<UUID, Warehouse> warehousesById) {
        Customer customer = customersById.get(order.getCustomerId());
        Warehouse warehouse = order.getConfirmedWarehouseId() == null ? null : warehousesById.get(order.getConfirmedWarehouseId());
        int itemCount = salesOrderItemRepository.findBySalesOrderId(order.getId()).size();
        int returnCount = (int) salesReturnRepository.findBySalesOrderId(order.getId()).stream().map(SalesReturn::getId).count();

        return new SalesOrderReportItemDto(
                order.getId(),
                order.getSoNumber(),
                order.getStatus().name(),
                order.getOrderDate(),
                order.getTotalAmount(),
                order.getCustomerId(),
                customer == null ? null : customer.getCode(),
                customer == null ? null : customer.getName(),
                order.getConfirmedWarehouseId(),
                warehouse == null ? null : warehouse.getCode(),
                warehouse == null ? null : warehouse.getName(),
                itemCount,
                returnCount);
    }

    private boolean isWithinDateRange(LocalDate value, LocalDate startDate, LocalDate endDate) {
        if (value == null) {
            return startDate == null && endDate == null;
        }

        if (startDate != null && value.isBefore(startDate)) {
            return false;
        }

        if (endDate != null && value.isAfter(endDate)) {
            return false;
        }

        return true;
    }

    private boolean hasMatchingStatus(Enum<?> statusValue, String expectedStatus) {
        return expectedStatus == null
                || expectedStatus.isBlank()
                || (statusValue != null && statusValue.name().equalsIgnoreCase(expectedStatus));
    }

    private <T> Map<UUID, T> toMapById(List<T> items, Function<T, UUID> idExtractor) {
        return items.stream()
                .filter(Objects::nonNull)
                .filter(item -> idExtractor.apply(item) != null)
                .collect(Collectors.toMap(idExtractor, Function.identity(), (left, right) -> left));
    }
}
