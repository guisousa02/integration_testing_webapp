package vvs_dbsetup;

import java.sql.SQLException;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.Operations;
import com.ninja_squad.dbsetup.destination.Destination;
import com.ninja_squad.dbsetup.destination.DriverManagerDestination;
import com.ninja_squad.dbsetup.operation.Operation;

import static vvs_dbsetup.DBSetupUtils.*;

import webapp.persistence.SaleRowDataGateway;
import webapp.services.*;

public class CustomersDBTest {

	private static Destination dataSource;
	
    // the tracker is static because JUnit uses a separate Test instance for every test method.
    private static DbSetupTracker dbSetupTracker = new DbSetupTracker();
	
    @BeforeAll
    public static void setupClass() {
//    	System.out.println("setup Class()... ");
    	
    	startApplicationDatabaseForTesting();
		dataSource = DriverManagerDestination.with(DB_URL, DB_USERNAME, DB_PASSWORD);
    }
    
	@BeforeEach
	public void setup() throws SQLException {
//		System.out.print("setup()... ");
		
		Operation initDBOperations = Operations.sequenceOf(
			DELETE_ALL,
			INSERT_CUSTOMER_SALE_DATA,
			INSERT_CUSTOMER_ADDRESS_DATA
			);
		
		DbSetup dbSetup = new DbSetup(dataSource, initDBOperations);
		
        // Use the tracker to launch the DBSetup. This will speed-up tests 
		// that do not change the DB. Otherwise, just use dbSetup.launch();
        dbSetupTracker.launchIfNecessary(dbSetup);
//		dbSetup.launch();
	}
	
	private boolean hasClient(int vat) throws ApplicationException {	
		CustomersDTO customersDTO = CustomerService.INSTANCE.getAllCustomers();
		
		for(CustomerDTO customer : customersDTO.customers)
			if (customer.vat == vat)
				return true;			
		return false;
	}
	
	@Test
	public void addCustomerWithExistingVATShouldFail() throws ApplicationException {

		dbSetupTracker.skipNextLaunch();
		int existingVAT = 197672337;

		assumeTrue(hasClient(existingVAT));

		assertThrows(ApplicationException.class, () -> {
			CustomerService.INSTANCE.addCustomer(existingVAT, "Duplicated Client", 912345678);
		});
	}

	@Test
	public void updateCustomerContactIsPersisted() throws ApplicationException {

		dbSetupTracker.skipNextLaunch();

		int vat = 197672337;
		int newPhone = 999999999;

		assumeTrue(hasClient(vat));

		CustomerService.INSTANCE.updateCustomerPhone(vat, newPhone);

		CustomerDTO updatedCustomer = CustomerService.INSTANCE.getCustomerByVat(vat);

		assertEquals(newPhone, updatedCustomer.phoneNumber);
	}

	@Test
	public void deletingAllCustomersLeavesDatabaseEmpty() throws ApplicationException {
		
		java.util.List<CustomerDTO> allCustomers = CustomerService.INSTANCE.getAllCustomers().customers;
		assumeTrue(allCustomers.size() > 0);

		for (CustomerDTO customer : allCustomers) {
			CustomerService.INSTANCE.removeCustomer(customer.vat);
		}

		int finalSize = CustomerService.INSTANCE.getAllCustomers().customers.size();
		assertEquals(0, finalSize);
	}

	@Test
	public void deleteAndAddCustomerAgainShouldWork() throws ApplicationException {

		int vat = 197672337;
		String name = "JOSE FARIA";
		int phone = 914276732;

		assumeTrue(hasClient(vat));

		CustomerService.INSTANCE.removeCustomer(vat);
		assertFalse(hasClient(vat));

		CustomerService.INSTANCE.addCustomer(vat, name, phone);
		assertTrue(hasClient(vat));
	}

	@Test
	public void removingCustomerRemovesAssociatedSales() throws ApplicationException {
		int vat = 197672337;

		assumeTrue(hasClient(vat));

		SalesDTO allSalesBeforeDTO = SaleService.INSTANCE.getAllSales();
		List<SaleDTO> allSalesBefore = allSalesBeforeDTO.sales;
		int totalSalesBefore = allSalesBefore.size();

		long salesOfCustomer = allSalesBefore.stream()
			.filter(sale -> sale.customerVat == vat)
			.count();

		assumeTrue(salesOfCustomer > 0);

		CustomerService.INSTANCE.removeCustomer(vat);

		SalesDTO allSalesAfterDTO = SaleService.INSTANCE.getAllSales();
		List<SaleDTO> allSalesAfter = allSalesAfterDTO.sales;
		int totalSalesAfter = allSalesAfter.size();

		assertEquals(totalSalesBefore - (int) salesOfCustomer, totalSalesAfter);

		boolean anySaleForCustomer = allSalesAfter.stream()
			.anyMatch(sale -> sale.customerVat == vat);
		assertFalse(anySaleForCustomer);
	}

	@Test
	public void addingSaleIncreasesTotalSalesByOne() throws ApplicationException {

		int customerVat = 197672337;
		assumeTrue(hasClient(customerVat));
		
		SalesDTO allSalesBeforeDTO = SaleService.INSTANCE.getAllSales();
		int totalSalesBefore = allSalesBeforeDTO.sales.size();
  
		SaleService.INSTANCE.addSale(customerVat);
  
		SalesDTO allSalesAfterDTO = SaleService.INSTANCE.getAllSales();
		int totalSalesAfter = allSalesAfterDTO.sales.size();
  
		assertEquals(totalSalesBefore + 1, totalSalesAfter);
	}
	
	// sale 1
	@Test
	public void systemCorrectlyReturnsAllSalesAssociatedWithCustomerVAT() throws ApplicationException {

		int customerVat = 197672337;
		assumeTrue(hasClient(customerVat));
  
		SalesDTO allSalesDTO = SaleService.INSTANCE.getAllSales();
		List<SaleDTO> allSales = allSalesDTO.sales;
  
		long expectedSalesCount = allSales.stream()
			.filter(sale -> sale.customerVat == customerVat)
			.count();
  
		assumeTrue(expectedSalesCount > 0);
  
		SalesDTO customerSalesDTO = SaleService.INSTANCE.getSaleByCustomerVat(customerVat);
		List<SaleDTO> customerSales = customerSalesDTO.sales;
  
		assertEquals(expectedSalesCount, customerSales.size());
  
		boolean allSalesHaveCorrectVAT = customerSales.stream()
			.allMatch(sale -> sale.customerVat == customerVat);
		
		assertTrue(allSalesHaveCorrectVAT);
	}
	
	// sale 2
	@Test
	public void canChangeSaleStatusFromOpenToClosed() throws ApplicationException {

		SalesDTO allSalesDTO = SaleService.INSTANCE.getAllSales();
		List<SaleDTO> allSales = allSalesDTO.sales;
		
		SaleDTO openSale = allSales.stream()
			.filter(sale -> sale.statusId.equals("O"))
			.findFirst()
			.orElse(null);
		
		assumeTrue(openSale != null);
		
		SaleService.INSTANCE.updateSale(openSale.id);
		
		SalesDTO updatedSalesDTO = SaleService.INSTANCE.getAllSales();
		SaleDTO updatedSale = updatedSalesDTO.sales.stream()
			.filter(sale -> sale.id == openSale.id)
			.findFirst()
			.orElse(null);
		
		assertNotNull(updatedSale);
		assertEquals("C", updatedSale.statusId);
	}
	
	// sale delivery 1
	@Test
	public void shouldNotAllowSaleDeliveryWithNonExistentSaleId() throws ApplicationException {
		SalesDTO allSalesDTO = SaleService.INSTANCE.getAllSales();
		List<SaleDTO> allSales = allSalesDTO.sales;
		
		int maxSaleId = allSales.stream()
			.mapToInt(sale -> sale.id)
			.max()
			.orElse(0);
		
		int nonExistentSaleId = maxSaleId + 1;
		
		int addressId = 100;
		
		assertThrows(ApplicationException.class, () -> {
			SaleService.INSTANCE.addSaleDelivery(nonExistentSaleId, addressId);
		});
	}
	
	// sale delivery 2
	@Test
	public void shouldCorrectlyAssociateSaleDeliveryWithSaleAddressAndCustomer() throws ApplicationException {

		int customerVat = 197672337;
		assumeTrue(hasClient(customerVat));

		SaleService.INSTANCE.addSale(customerVat);

		SalesDTO customerSalesDTO = SaleService.INSTANCE.getSaleByCustomerVat(customerVat);
		List<SaleDTO> customerSales = customerSalesDTO.sales;
		assumeTrue(!customerSales.isEmpty());

		SaleDTO newSale = customerSales.get(customerSales.size() - 1);

		int addressId = 100;

		int returnedCustomerVat = SaleService.INSTANCE.addSaleDelivery(newSale.id, addressId);

		assertEquals(customerVat, returnedCustomerVat);

		SalesDeliveryDTO salesDeliveryDTO = SaleService.INSTANCE.getSalesDeliveryByVat(customerVat);
		List<SaleDeliveryDTO> deliveries = salesDeliveryDTO.sales_delivery;

		SaleDeliveryDTO delivery = deliveries.stream()
			.filter(d -> d.sale_id == newSale.id && d.addr_id == addressId)
			.findFirst()
			.orElse(null);

		assertNotNull(delivery);
		assertEquals(newSale.id, delivery.sale_id);
		assertEquals(addressId, delivery.addr_id);
		assertEquals(customerVat, delivery.customer_vat);
	}
}
