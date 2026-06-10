package com.maiu.erp.modules.reporting.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.maiu.erp.modules.inventory.application.service.InventoryService;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.InventoryStock;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrder;
import com.maiu.erp.modules.inventory.domain.model.PurchaseOrderItem;
import com.maiu.erp.modules.inventory.domain.model.PurchaseReturn;
import com.maiu.erp.modules.inventory.domain.model.SalesOrder;
import com.maiu.erp.modules.inventory.domain.model.SalesOrderItem;
import com.maiu.erp.modules.inventory.domain.model.SalesReturn;
import com.maiu.erp.modules.inventory.domain.model.StockMovement;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.PurchaseReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderItemRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesOrderRepository;
import com.maiu.erp.modules.inventory.domain.repository.SalesReturnRepository;
import com.maiu.erp.modules.inventory.domain.repository.StockMovementRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
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
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.PurchaseOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.ReportLineItemDto;
import com.maiu.erp.modules.reporting.application.dto.ReportMovementItemDto;
import com.maiu.erp.modules.reporting.application.dto.ReportReturnDto;
import com.maiu.erp.modules.reporting.application.dto.ReportingOverviewDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportItemDto;
import com.maiu.erp.modules.reporting.application.dto.SalesOrderReportSummaryDto;
import com.maiu.erp.modules.reporting.application.dto.StockMovementReportDetailDto;
import com.maiu.erp.modules.reporting.application.dto.StockMovementReportSummaryDto;
import com.maiu.erp.shared.exception.NotFoundException;

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
    private final StockMovementRepository stockMovementRepository;

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
            CustomerRepository customerRepository,
            StockMovementRepository stockMovementRepository) {
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
        this.stockMovementRepository = stockMovementRepository;
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
            UUID supplierId,
            int page,
            int size) {
        Map<UUID, Project> projectsById = toMapById(projectRepository.findAll(), Project::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Supplier> suppliersById = toMapById(supplierRepository.findAll(), Supplier::getId);

        List<PurchaseOrderReportItemDto> filteredOrders = purchaseOrderRepository.findAll().stream()
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

        return buildPurchaseOrderSummary(startDate, endDate, filteredOrders, page, size);
    }

    public PurchaseOrderReportDetailDto getPurchaseOrderReportDetail(UUID purchaseOrderId) {
        PurchaseOrder order = purchaseOrderRepository.findById(purchaseOrderId)
                .orElseThrow(() -> new NotFoundException("Purchase order not found"));
        Map<UUID, Project> projectsById = toMapById(projectRepository.findAll(), Project::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Supplier> suppliersById = toMapById(supplierRepository.findAll(), Supplier::getId);
        Map<UUID, Product> productsById = toMapById(productRepository.findAll(), Product::getId);

        PurchaseOrderReportItemDto summary = toPurchaseOrderReportItem(order, suppliersById, projectsById, warehousesById);
        List<ReportLineItemDto> items = purchaseOrderItemRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .map(item -> toReportLineItem(item, productsById))
                .toList();
        List<ReportReturnDto> returns = purchaseReturnRepository.findByPurchaseOrderId(purchaseOrderId).stream()
                .map(returnEntry -> toReportReturn(returnEntry, warehousesById))
                .toList();

        return new PurchaseOrderReportDetailDto(summary, items, returns);
    }

    public String exportPurchaseOrderReportCsv(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID projectId,
            UUID supplierId) {
        PurchaseOrderReportSummaryDto summary = getPurchaseOrderReport(
                startDate,
                endDate,
                status,
                projectId,
                supplierId,
                0,
                Integer.MAX_VALUE);

        List<String> rows = new ArrayList<>();
        rows.add(csvRow("PO Number", "Order Date", "Status", "Supplier", "Supplier Code", "Project", "Warehouse", "Items", "Returns", "Total Amount"));
        summary.getOrders().forEach(order -> rows.add(csvRow(
                order.getPoNumber(),
                order.getOrderDate(),
                order.getStatus(),
                order.getSupplierName(),
                order.getSupplierCode(),
                order.getProjectCode(),
                order.getReceivedWarehouseCode(),
                order.getItemCount(),
                order.getReturnCount(),
                order.getTotalAmount())));
        return String.join("\n", rows);
    }

    public SalesOrderReportSummaryDto getSalesOrderReport(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID customerId,
            UUID warehouseId,
            int page,
            int size) {
        Map<UUID, Customer> customersById = toMapById(customerRepository.findAll(), Customer::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);

        List<SalesOrderReportItemDto> filteredOrders = salesOrderRepository.findAll().stream()
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

        return buildSalesOrderSummary(startDate, endDate, filteredOrders, page, size);
    }

    public SalesOrderReportDetailDto getSalesOrderReportDetail(UUID salesOrderId) {
        SalesOrder order = salesOrderRepository.findById(salesOrderId)
                .orElseThrow(() -> new NotFoundException("Sales order not found"));
        Map<UUID, Customer> customersById = toMapById(customerRepository.findAll(), Customer::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Product> productsById = toMapById(productRepository.findAll(), Product::getId);

        SalesOrderReportItemDto summary = toSalesOrderReportItem(order, customersById, warehousesById);
        List<ReportLineItemDto> items = salesOrderItemRepository.findBySalesOrderId(salesOrderId).stream()
                .map(item -> toReportLineItem(item, productsById))
                .toList();
        List<ReportReturnDto> returns = salesReturnRepository.findBySalesOrderId(salesOrderId).stream()
                .map(returnEntry -> toReportReturn(returnEntry, warehousesById))
                .toList();

        return new SalesOrderReportDetailDto(summary, items, returns);
    }

    public String exportSalesOrderReportCsv(
            LocalDate startDate,
            LocalDate endDate,
            String status,
            UUID customerId,
            UUID warehouseId) {
        SalesOrderReportSummaryDto summary = getSalesOrderReport(
                startDate,
                endDate,
                status,
                customerId,
                warehouseId,
                0,
                Integer.MAX_VALUE);

        List<String> rows = new ArrayList<>();
        rows.add(csvRow("SO Number", "Order Date", "Status", "Customer", "Customer Code", "Warehouse", "Items", "Returns", "Total Amount"));
        summary.getOrders().forEach(order -> rows.add(csvRow(
                order.getSoNumber(),
                order.getOrderDate(),
                order.getStatus(),
                order.getCustomerName(),
                order.getCustomerCode(),
                order.getConfirmedWarehouseCode(),
                order.getItemCount(),
                order.getReturnCount(),
                order.getTotalAmount())));
        return String.join("\n", rows);
    }

    public StockMovementReportSummaryDto getStockMovementReport(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            String referenceType,
            String movementType,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size) {
        List<StockMovement> movements = inventoryService.getStockMovements(
                productId,
                warehouseId,
                projectId,
                null,
                referenceType,
                movementType,
                startDate,
                endDate);

        Map<UUID, Product> productsById = toMapById(productRepository.findAll(), Product::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Project> projectsById = toMapById(projectRepository.findAll(), Project::getId);

        List<ReportMovementItemDto> items = movements.stream()
                .map(movement -> toMovementItem(movement, productsById, warehousesById, projectsById))
                .toList();
        List<ReportMovementItemDto> pagedItems = paginate(items, page, size);

        BigDecimal totalQuantity = items.stream()
                .map(ReportMovementItemDto::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new StockMovementReportSummaryDto(
                startDate,
                endDate,
                items.size(),
                totalQuantity,
                page,
                size,
                items.size(),
                calculateTotalPages(items.size(), size),
                pagedItems);
    }

    public StockMovementReportDetailDto getStockMovementReportDetail(UUID stockMovementId) {
        StockMovement movement = stockMovementRepository.findAll().stream()
                .filter(Objects::nonNull)
                .filter(candidate -> stockMovementId.equals(candidate.getId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Stock movement not found"));
        Map<UUID, Product> productsById = toMapById(productRepository.findAll(), Product::getId);
        Map<UUID, Warehouse> warehousesById = toMapById(warehouseRepository.findAll(), Warehouse::getId);
        Map<UUID, Project> projectsById = toMapById(projectRepository.findAll(), Project::getId);

        ReportMovementItemDto summary = toMovementItem(movement, productsById, warehousesById, projectsById);
        BigDecimal totalValue = movement.getQuantity() == null || movement.getUnitCost() == null
                ? BigDecimal.ZERO
                : movement.getQuantity().multiply(movement.getUnitCost());
        return new StockMovementReportDetailDto(summary, totalValue);
    }

    public String exportStockMovementReportCsv(
            UUID productId,
            UUID warehouseId,
            UUID projectId,
            String referenceType,
            String movementType,
            LocalDate startDate,
            LocalDate endDate) {
        StockMovementReportSummaryDto summary = getStockMovementReport(
                productId,
                warehouseId,
                projectId,
                referenceType,
                movementType,
                startDate,
                endDate,
                0,
                Integer.MAX_VALUE);

        List<String> rows = new ArrayList<>();
        rows.add(csvRow("Date", "Movement Type", "Product", "Product SKU", "Warehouse", "Warehouse Code", "Project", "Reference Type", "Quantity", "Unit Cost"));
        summary.getMovements().forEach(movement -> rows.add(csvRow(
                movement.getMovementDate(),
                movement.getMovementType(),
                movement.getProductName(),
                movement.getProductSku(),
                movement.getWarehouseName(),
                movement.getWarehouseCode(),
                movement.getProjectCode(),
                movement.getReferenceType(),
                movement.getQuantity(),
                movement.getUnitCost())));
        return String.join("\n", rows);
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
                .filter(movement -> movement.getMovementType() != null)
                .collect(Collectors.groupingBy(movement -> movement.getMovementType().name(), Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(4)
                .map(entry -> new MovementTypeCountDto(entry.getKey(), entry.getValue().intValue()))
                .toList();

        List<ReportMovementItemDto> recentMovements = movements.stream()
                .sorted(Comparator.comparing(StockMovement::getMovementDate, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
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

    private PurchaseOrderReportSummaryDto buildPurchaseOrderSummary(
            LocalDate startDate,
            LocalDate endDate,
            List<PurchaseOrderReportItemDto> filteredOrders,
            int page,
            int size) {
        BigDecimal totalAmount = filteredOrders.stream()
                .map(PurchaseOrderReportItemDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<PurchaseOrderReportItemDto> pagedOrders = paginate(filteredOrders, page, size);

        return new PurchaseOrderReportSummaryDto(
                startDate,
                endDate,
                filteredOrders.size(),
                totalAmount,
                page,
                size,
                filteredOrders.size(),
                calculateTotalPages(filteredOrders.size(), size),
                pagedOrders);
    }

    private SalesOrderReportSummaryDto buildSalesOrderSummary(
            LocalDate startDate,
            LocalDate endDate,
            List<SalesOrderReportItemDto> filteredOrders,
            int page,
            int size) {
        BigDecimal totalAmount = filteredOrders.stream()
                .map(SalesOrderReportItemDto::getTotalAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalesOrderReportItemDto> pagedOrders = paginate(filteredOrders, page, size);

        return new SalesOrderReportSummaryDto(
                startDate,
                endDate,
                filteredOrders.size(),
                totalAmount,
                page,
                size,
                filteredOrders.size(),
                calculateTotalPages(filteredOrders.size(), size),
                pagedOrders);
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
        int returnCount = purchaseReturnRepository.findByPurchaseOrderId(order.getId()).size();

        return new PurchaseOrderReportItemDto(
                order.getId(),
                order.getPoNumber(),
                order.getStatus() == null ? null : order.getStatus().name(),
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
        int returnCount = salesReturnRepository.findBySalesOrderId(order.getId()).size();

        return new SalesOrderReportItemDto(
                order.getId(),
                order.getSoNumber(),
                order.getStatus() == null ? null : order.getStatus().name(),
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
                movement.getMovementType() == null ? null : movement.getMovementType().name(),
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

    private ReportLineItemDto toReportLineItem(PurchaseOrderItem item, Map<UUID, Product> productsById) {
        Product product = productsById.get(item.getProductId());
        return new ReportLineItemDto(
                item.getProductId(),
                product == null ? null : product.getSku(),
                product == null ? null : product.getName(),
                item.getQuantity(),
                item.getUnitCost(),
                item.getLineTotal());
    }

    private ReportLineItemDto toReportLineItem(SalesOrderItem item, Map<UUID, Product> productsById) {
        Product product = productsById.get(item.getProductId());
        return new ReportLineItemDto(
                item.getProductId(),
                product == null ? null : product.getSku(),
                product == null ? null : product.getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getLineTotal());
    }

    private ReportReturnDto toReportReturn(PurchaseReturn returnEntry, Map<UUID, Warehouse> warehousesById) {
        Warehouse warehouse = warehousesById.get(returnEntry.getWarehouseId());
        return new ReportReturnDto(
                returnEntry.getId(),
                returnEntry.getReturnNumber(),
                returnEntry.getWarehouseId(),
                warehouse == null ? null : warehouse.getCode(),
                warehouse == null ? null : warehouse.getName(),
                returnEntry.getRemarks(),
                returnEntry.getPerformedBy(),
                returnEntry.getReturnedAt());
    }

    private ReportReturnDto toReportReturn(SalesReturn returnEntry, Map<UUID, Warehouse> warehousesById) {
        Warehouse warehouse = warehousesById.get(returnEntry.getWarehouseId());
        return new ReportReturnDto(
                returnEntry.getId(),
                returnEntry.getReturnNumber(),
                returnEntry.getWarehouseId(),
                warehouse == null ? null : warehouse.getCode(),
                warehouse == null ? null : warehouse.getName(),
                returnEntry.getRemarks(),
                returnEntry.getPerformedBy(),
                returnEntry.getReturnedAt());
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

    private <T> List<T> paginate(List<T> items, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        int fromIndex = Math.min(safePage * safeSize, items.size());
        int toIndex = Math.min(fromIndex + safeSize, items.size());
        return items.subList(fromIndex, toIndex);
    }

    private int calculateTotalPages(long totalElements, int size) {
        int safeSize = size <= 0 ? 20 : size;
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / safeSize);
    }

    private String csvRow(Object... values) {
        return java.util.Arrays.stream(values)
                .map(this::csvValue)
                .collect(Collectors.joining(","));
    }

    private String csvValue(Object value) {
        if (value == null) {
            return "";
        }

        String stringValue = String.valueOf(value).replace("\"", "\"\"");
        if (stringValue.contains(",") || stringValue.contains("\"") || stringValue.contains("\n")) {
            return "\"" + stringValue + "\"";
        }

        return stringValue;
    }
}
