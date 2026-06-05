package com.maiu.erp.shared.seed;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.maiu.erp.modules.identity.application.service.DefaultRoleService;
import com.maiu.erp.modules.identity.application.service.DefaultTenantService;
import com.maiu.erp.modules.identity.domain.model.Role;
import com.maiu.erp.modules.identity.domain.model.User;
import com.maiu.erp.modules.identity.domain.repository.UserRepository;
import com.maiu.erp.modules.inventory.domain.model.Category;
import com.maiu.erp.modules.inventory.domain.model.Customer;
import com.maiu.erp.modules.inventory.domain.model.Product;
import com.maiu.erp.modules.inventory.domain.model.Supplier;
import com.maiu.erp.modules.inventory.domain.model.Unit;
import com.maiu.erp.modules.inventory.domain.model.Warehouse;
import com.maiu.erp.modules.inventory.domain.repository.CategoryRepository;
import com.maiu.erp.modules.inventory.domain.repository.CustomerRepository;
import com.maiu.erp.modules.inventory.domain.repository.ProductRepository;
import com.maiu.erp.modules.inventory.domain.repository.SupplierRepository;
import com.maiu.erp.modules.inventory.domain.repository.UnitRepository;
import com.maiu.erp.modules.inventory.domain.repository.WarehouseRepository;
import com.maiu.erp.modules.project.domain.enums.ProjectStatus;
import com.maiu.erp.modules.project.domain.model.Project;
import com.maiu.erp.modules.project.domain.model.ProjectBudgetLine;
import com.maiu.erp.modules.project.domain.repository.ProjectBudgetLineRepository;
import com.maiu.erp.modules.project.domain.repository.ProjectRepository;

@Service
public class UatSeedService {
    private static final Logger log = LoggerFactory.getLogger(UatSeedService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultRoleService defaultRoleService;
    private final DefaultTenantService defaultTenantService;
    private final CategoryRepository categoryRepository;
    private final UnitRepository unitRepository;
    private final WarehouseRepository warehouseRepository;
    private final SupplierRepository supplierRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;
    private final ProjectBudgetLineRepository projectBudgetLineRepository;

    public UatSeedService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DefaultRoleService defaultRoleService,
            DefaultTenantService defaultTenantService,
            CategoryRepository categoryRepository,
            UnitRepository unitRepository,
            WarehouseRepository warehouseRepository,
            SupplierRepository supplierRepository,
            CustomerRepository customerRepository,
            ProductRepository productRepository,
            ProjectRepository projectRepository,
            ProjectBudgetLineRepository projectBudgetLineRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultRoleService = defaultRoleService;
        this.defaultTenantService = defaultTenantService;
        this.categoryRepository = categoryRepository;
        this.unitRepository = unitRepository;
        this.warehouseRepository = warehouseRepository;
        this.supplierRepository = supplierRepository;
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
        this.projectRepository = projectRepository;
        this.projectBudgetLineRepository = projectBudgetLineRepository;
    }

    public void seed(String sharedPassword) {
        log.info("Seeding UAT baseline data");

        seedUsers(sharedPassword);

        Category cementCategory = upsertCategory("Cement and Concrete");
        Category electricalCategory = upsertCategory("Electrical");
        Category finishingCategory = upsertCategory("Finishing");

        Unit pcs = upsertUnit("pcs", "pcs");
        Unit bag = upsertUnit("bag", "bag");
        Unit roll = upsertUnit("roll", "roll");

        upsertWarehouse("WH-MAIN", "Main Warehouse", "Main yard and receiving area");
        upsertWarehouse("WH-SITE", "Site Buffer Warehouse", "Temporary project staging area");

        upsertSupplier(
                "SUP-UAT-001",
                "ABC Construction Supply",
                "Ana Cruz",
                "09170000001",
                "abc.supply@maiu.local",
                "City Industrial Park");
        upsertSupplier(
                "SUP-UAT-002",
                "Metro Electrical Trading",
                "Marco Reyes",
                "09170000002",
                "metro.electrical@maiu.local",
                "Electrical Hub District");

        Customer customer = upsertCustomer(
                "CUS-UAT-001",
                "Internal Construction Client",
                "Project Sponsor",
                "09170000003",
                "client.uat@maiu.local",
                "Main Office");

        upsertProduct(
                "CEM-001",
                "Portland Cement 40kg",
                "General concrete works",
                new BigDecimal("285.00"),
                new BigDecimal("320.00"),
                cementCategory.getId(),
                bag.getId());
        upsertProduct(
                "REB-010",
                "Rebar 10mm",
                "Structural reinforcement",
                new BigDecimal("420.00"),
                new BigDecimal("470.00"),
                cementCategory.getId(),
                pcs.getId());
        upsertProduct(
                "WIRE-250",
                "Electrical Wire 2.5mm",
                "General branch circuit wiring",
                new BigDecimal("1650.00"),
                new BigDecimal("1850.00"),
                electricalCategory.getId(),
                roll.getId());
        upsertProduct(
                "PLY-001",
                "Marine Plywood 12mm",
                "Formwork and finishing support",
                new BigDecimal("980.00"),
                new BigDecimal("1120.00"),
                finishingCategory.getId(),
                pcs.getId());

        Project project = upsertProject(customer.getId());
        upsertBudgetLine(project.getId(), "CIVIL", "Civil Works", new BigDecimal("220000.00"));
        upsertBudgetLine(project.getId(), "ELEC", "Electrical Works", new BigDecimal("120000.00"));
        upsertBudgetLine(project.getId(), "FINISH", "Finishing Works", new BigDecimal("160000.00"));
    }

    private void seedUsers(String sharedPassword) {
        Role userRole = defaultRoleService.getOrCreateUserRole();
        Role adminRole = defaultRoleService.getOrCreateAdminRole();
        Role projectManagerRole = defaultRoleService.getOrCreateProjectManagerRole();
        Role procurementRole = defaultRoleService.getOrCreateProcurementRole();
        Role warehouseRole = defaultRoleService.getOrCreateWarehouseRole();
        Role viewerRole = defaultRoleService.getOrCreateViewerRole();

        upsertUser("Default Admin", "admin@maiu.local", sharedPassword, Set.of(userRole, adminRole));
        upsertUser("Project Lead", "pm.uat@maiu.local", sharedPassword, Set.of(userRole, projectManagerRole));
        upsertUser("Procurement Officer", "procurement.uat@maiu.local", sharedPassword,
                Set.of(userRole, procurementRole));
        upsertUser("Warehouse Officer", "warehouse.uat@maiu.local", sharedPassword,
                Set.of(userRole, warehouseRole));
        upsertUser("Read Only User", "viewer.uat@maiu.local", sharedPassword, Set.of(userRole, viewerRole));
    }

    private void upsertUser(String name, String email, String rawPassword, Set<Role> roles) {
        String normalizedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(User::new);
        user.setName(name);
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRoles(roles);
        user.setTenantId(defaultTenantService.getDefaultTenantId());
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private Category upsertCategory(String name) {
        Category category = categoryRepository.findAll().stream()
                .filter(existing -> existing.getName() != null && existing.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElseGet(Category::new);
        category.setName(name);
        category.setActive(true);
        return categoryRepository.save(category);
    }

    private Unit upsertUnit(String name, String symbol) {
        Unit unit = unitRepository.findByName(name).orElseGet(Unit::new);
        unit.setName(name);
        unit.setSymbol(symbol);
        unit.setActive(true);
        return unitRepository.save(unit);
    }

    private Warehouse upsertWarehouse(String code, String name, String address) {
        Warehouse warehouse = warehouseRepository.findByCode(code).orElseGet(Warehouse::new);
        warehouse.setCode(code);
        warehouse.setName(name);
        warehouse.setAddress(address);
        warehouse.setActive(true);
        return warehouseRepository.save(warehouse);
    }

    private Supplier upsertSupplier(
            String code,
            String name,
            String contactPerson,
            String phone,
            String email,
            String address) {
        Supplier supplier = supplierRepository.findByCode(code).orElseGet(Supplier::new);
        supplier.setCode(code);
        supplier.setName(name);
        supplier.setContactPerson(contactPerson);
        supplier.setPhone(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setActive(true);
        return supplierRepository.save(supplier);
    }

    private Customer upsertCustomer(
            String code,
            String name,
            String contactPerson,
            String phone,
            String email,
            String address) {
        Customer customer = customerRepository.findByCode(code).orElseGet(Customer::new);
        customer.setCode(code);
        customer.setName(name);
        customer.setContactPerson(contactPerson);
        customer.setPhone(phone);
        customer.setEmail(email);
        customer.setAddress(address);
        customer.setActive(true);
        return customerRepository.save(customer);
    }

    private Product upsertProduct(
            String sku,
            String name,
            String description,
            BigDecimal costPrice,
            BigDecimal sellingPrice,
            UUID categoryId,
            UUID unitId) {
        Product product = productRepository.findBySku(sku).orElseGet(Product::new);
        Instant now = Instant.now();

        product.setSku(sku);
        product.setName(name);
        product.setDescription(description);
        product.setCostPrice(costPrice);
        product.setSellingPrice(sellingPrice);
        product.setCategoryId(categoryId);
        product.setUnitId(unitId);
        product.setActive(true);
        if (product.getCreatedAt() == null) {
            product.setCreatedAt(now);
        }
        product.setUpdatedAt(now);

        return productRepository.save(product);
    }

    private Project upsertProject(UUID customerId) {
        Project project = projectRepository.findByProjectCode("PRJ-UAT-001").orElseGet(Project::new);
        Instant now = Instant.now();

        project.setProjectCode("PRJ-UAT-001");
        project.setProjectName("Two-Storey Residential Build - UAT");
        project.setCustomerId(customerId);
        project.setLocation("Sample Construction Site");
        project.setStartDate(LocalDate.now().minusDays(7));
        project.setTargetEndDate(LocalDate.now().plusMonths(4));
        project.setBudgetAmount(new BigDecimal("500000.00"));
        project.setStatus(ProjectStatus.ACTIVE);
        if (project.getCreatedAt() == null) {
            project.setCreatedAt(now);
        }
        project.setUpdatedAt(now);

        return projectRepository.save(project);
    }

    private ProjectBudgetLine upsertBudgetLine(UUID projectId, String costCode, String description,
            BigDecimal amount) {
        ProjectBudgetLine budgetLine = projectBudgetLineRepository.findByProjectIdAndCostCode(projectId, costCode)
                .orElseGet(ProjectBudgetLine::new);
        Instant now = Instant.now();

        budgetLine.setProjectId(projectId);
        budgetLine.setCostCode(costCode);
        budgetLine.setDescription(description);
        budgetLine.setBudgetAmount(amount);
        if (budgetLine.getCreatedAt() == null) {
            budgetLine.setCreatedAt(now);
        }
        budgetLine.setUpdatedAt(now);

        return projectBudgetLineRepository.save(budgetLine);
    }
}
