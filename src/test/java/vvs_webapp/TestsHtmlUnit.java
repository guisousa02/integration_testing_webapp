package vvs_webapp;

import static org.junit.Assert.*;
import org.junit.*;
import org.junit.runners.MethodSorters;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.*;

import java.util.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestsHtmlUnit {
	
	private static final String APPLICATION_URL = "http://localhost:8080/VVS_webappdemo/";

	private static WebClient webClient;
	private static HtmlPage page;
	
	@BeforeClass
	public static void setUpClass() throws Exception {
		webClient = new WebClient(BrowserVersion.getDefault());
		
		// possible configurations needed to prevent JUnit tests to fail for complex HTML pages
        webClient.setJavaScriptTimeout(15000);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        
		page = webClient.getPage(APPLICATION_URL);
		assertEquals(200, page.getWebResponse().getStatusCode()); // OK status
	}
	
	@AfterClass
	public static void takeDownClass() {
		webClient.close();
	}
	
	@Test
    public void testAddTwoAddressesToFirstCustomer() throws Exception {
        
        HtmlPage homePage = page;

        HtmlAnchor listCustomersLink = homePage.getAnchorByHref("GetAllCustomersPageController");
        HtmlPage customerListPage = listCustomersLink.click();

        HtmlTable customerTable = customerListPage.getHtmlElementById("clients");
        HtmlTableRow firstRow = customerTable.getRows().get(1);
        String vat = firstRow.getCell(2).asText();
        
        String[] addr = {"Rua Alfa", "Rua Beta"};
        HtmlPage result = null;

        for (int i = 0; i < addr.length; i++) {
        	HtmlAnchor addAddressLink = page.getAnchorByHref("addAddressToCustomer.html");
        	HtmlPage nextPage = addAddressLink.click();
            HtmlForm form = nextPage.getForms().get(0);

            form.getInputByName("vat").setValueAttribute(vat);
            form.getInputByName("address").setValueAttribute(addr[i]);
            form.getInputByName("door").setValueAttribute("1");
            form.getInputByName("postalCode").setValueAttribute("1000-000");
            form.getInputByName("locality").setValueAttribute("Lisboa");

            result = form.getInputByValue("Insert").click();
            assertTrue(result.asText().contains("Client Info"));
        }

        HtmlTable addressTable = (HtmlTable) result.getFirstByXPath("//table[@class='w3-table w3-bordered']");
        assertNotNull("Address table not found!", addressTable);

        List<String> addressTexts = new ArrayList<>();
        for (HtmlTableRow row : addressTable.getRows().subList(1, addressTable.getRowCount())) {
            addressTexts.add(row.asText());
        }

        assertTrue(addressTexts.stream().anyMatch(text -> text.contains("Rua Alfa")));
        assertTrue(addressTexts.stream().anyMatch(text -> text.contains("Rua Beta")));
    }
	
	@Test
    public void testAddTwoCustomers() throws Exception {
		String[] vat = {"203003195", "235457353"};
		String[] name = {"Manuel", "LinkedIn"};
		String[] phone = {"123456789", "987654321"};

		for(int i = 0; i < 2; i++) {
			HtmlAnchor addCustomerLink = page.getAnchorByHref("addCustomer.html");
			HtmlPage nextPage = (HtmlPage) addCustomerLink.openLinkInNewWindow();
			assertEquals("Enter Name", nextPage.getTitleText());
			
			HtmlForm addCustomerForm = nextPage.getForms().get(0);
			
			HtmlInput vatInput = addCustomerForm.getInputByName("vat");
			vatInput.setValueAttribute(vat[i]);
			
			HtmlInput nameInput = addCustomerForm.getInputByName("designation");
			nameInput.setValueAttribute(name[i]);
			
			HtmlInput phoneInput = addCustomerForm.getInputByName("phone");
			phoneInput.setValueAttribute(phone[i]);

			HtmlInput submitButton = addCustomerForm.getInputByName("submit");

			HtmlPage reportPage = submitButton.click();
            String textReportPage = reportPage.asText();

            assertTrue(textReportPage.contains(name[i]));
            assertTrue(textReportPage.contains(phone[i]));
		}

        for (int j = 0; j < 2; j++) {
            HtmlAnchor removeCustomerLink = page.getAnchorByHref("RemoveCustomerPageController");
            HtmlPage nextPage = (HtmlPage) removeCustomerLink.openLinkInNewWindow();
            assertTrue(nextPage.asText().contains(vat[j]));

            HtmlForm removeCustomerForm = nextPage.getForms().get(0);

            HtmlInput vatInput = removeCustomerForm.getInputByName("vat");
            vatInput.setValueAttribute(vat[j]);
            HtmlInput submit = removeCustomerForm.getInputByName("submit");
            HtmlPage reportPage = submit.click();
            assertFalse(reportPage.asText().contains(vat[j]));

            HtmlAnchor getCustomersLink = page.getAnchorByHref("GetAllCustomersPageController");
            nextPage = (HtmlPage) getCustomersLink.openLinkInNewWindow();
            assertFalse(nextPage.asText().contains(vat[j]));
        }
	}
	
	@Test
    public void testAddSale() throws Exception {

        HtmlPage homePage = page;

        HtmlAnchor listCustomersLink = homePage.getAnchorByHref("GetAllCustomersPageController");
        HtmlPage customerListPage = listCustomersLink.click();

        HtmlTable customerTable = customerListPage.getHtmlElementById("clients");
        HtmlTableRow firstRow = customerTable.getRows().get(1);
        String vat = firstRow.getCell(2).asText();

        HtmlAnchor newSale = homePage.getAnchorByHref("addSale.html");
        HtmlPage newSalePage = newSale.click();
        assertEquals("New Sale", newSalePage.getTitleText());

        HtmlForm form = newSalePage.getForms().get(0);

        HtmlInput vatInput = form.getInputByName("customerVat");
        vatInput.setValueAttribute(vat);

        HtmlInput submit = form.getInputByValue("Add Sale");
        HtmlPage reportPage = submit.click();

        assertEquals("Sales Info", reportPage.getTitleText());
        assertTrue(reportPage.asText().contains(vat));
	}

    @Test
    public void testCloseSale() throws Exception {
        HtmlPage homePage = page;

        HtmlAnchor updateSalesStatus = homePage.getAnchorByHref("UpdateSaleStatusPageControler");
        HtmlPage updateSalesStatusPage = updateSalesStatus.click();
        assertEquals("Enter Sale Id", updateSalesStatusPage.getTitleText());

        HtmlTable salesTable = (HtmlTable) updateSalesStatusPage.getFirstByXPath("//table[contains(@class, 'w3-table')]");
        assertNotNull("Sales table not found!", salesTable);

        HtmlTableRow firstValidRow = salesTable.getRows().get(2);

        String saleId = firstValidRow.getCell(0).asText();

        HtmlForm form = updateSalesStatusPage.getForms().get(0);
        HtmlInput saleIdInput = form.getInputByName("id");
        saleIdInput.setValueAttribute(saleId);
        
        HtmlInput submit = form.getInputByValue("Close Sale");
        HtmlPage reportPage = submit.click();
        assertEquals("Enter Sale Id", reportPage.getTitleText());

        HtmlTable updatedSalesTable = (HtmlTable) reportPage.getFirstByXPath("//table[@class='w3-table w3-bordered']");
        assertNotNull("Updated sales table not found!", updatedSalesTable);

        HtmlTableRow firstValidRow2 = updatedSalesTable.getRows().get(2);

        String updatedSaleStatus = firstValidRow2.getCell(3).asText();
        assertEquals("C", updatedSaleStatus);
    }
    
    @Test
    public void testNewCustSaleDeli() throws Exception {
    	String vat = "123456789";
		String name = "Gabriela Silva";
		String phone = "912345678";
        String address = "Rua Cidade da Praia";
        String door = "50";
        String postalCode = "1800-000";
        String locality = "Lisboa";

		HtmlAnchor addCustomerLink = page.getAnchorByHref("addCustomer.html");
		HtmlPage nextPage = (HtmlPage) addCustomerLink.openLinkInNewWindow();
		assertEquals("Enter Name", nextPage.getTitleText());
		
		HtmlForm addCustomerForm = nextPage.getForms().get(0);
		
		HtmlInput vatInput = addCustomerForm.getInputByName("vat");
		vatInput.setValueAttribute(vat);
		
		HtmlInput nameInput = addCustomerForm.getInputByName("designation");
		nameInput.setValueAttribute(name);
		
		HtmlInput phoneInput = addCustomerForm.getInputByName("phone");
		phoneInput.setValueAttribute(phone);

		HtmlInput submitButton = addCustomerForm.getInputByName("submit");
        submitButton.click();

        HtmlAnchor addSaleLink = page.getAnchorByHref("addAddressToCustomer.html");
        HtmlPage addSalePage = addSaleLink.click();
        assertEquals("Enter Address", addSalePage.getTitleText());

        HtmlForm addSaleForm = addSalePage.getForms().get(0);

        HtmlInput vatInput2 = addSaleForm.getInputByName("vat");
        vatInput2.setValueAttribute(vat);

        HtmlInput addressInput = addSaleForm.getInputByName("address");
        addressInput.setValueAttribute(address);

        HtmlInput doorInput = addSaleForm.getInputByName("door");
        doorInput.setValueAttribute(door);

        HtmlInput postalCodeInput = addSaleForm.getInputByName("postalCode");
        postalCodeInput.setValueAttribute(postalCode);

        HtmlInput localityInput = addSaleForm.getInputByName("locality");
        localityInput.setValueAttribute(locality);

        HtmlInput submitButton2 = addSaleForm.getInputByValue("Insert");
        HtmlPage reportPage2 = submitButton2.click();
        assertTrue(reportPage2.asText().contains("Customer Info"));
        assertTrue(reportPage2.asText().contains(name));

        HtmlAnchor addSaleLink2 = page.getAnchorByHref("addSale.html");
        HtmlPage addSalePage2 = addSaleLink2.click();
        assertEquals("New Sale", addSalePage2.getTitleText());

        HtmlForm addSaleForm2 = addSalePage2.getForms().get(0);

        HtmlInput vatInput3 = addSaleForm2.getInputByName("customerVat");
        vatInput3.setValueAttribute(vat);

        HtmlInput submitButton3 = addSaleForm2.getInputByValue("Add Sale");
        HtmlPage reportPage3 = submitButton3.click();
        assertEquals("Sales Info", reportPage3.getTitleText());
        assertTrue(reportPage3.asText().contains(vat));

        HtmlAnchor updateSalesStatus = page.getAnchorByHref("saleDeliveryVat.html");
        HtmlPage updateSalesStatusPage = updateSalesStatus.click();
        assertEquals("Enter Name", updateSalesStatusPage.getTitleText());

        HtmlForm updateSalesStatusForm = updateSalesStatusPage.getForms().get(0);
        HtmlInput vatInput4 = updateSalesStatusForm.getInputByName("vat");
        vatInput4.setValueAttribute(vat);

        HtmlInput submitButton4 = updateSalesStatusForm.getInputByValue("Get Customer");
        HtmlPage reportPage4 = submitButton4.click();
        assertEquals("Enter Name", reportPage4.getTitleText());

        HtmlTable addressTable = (HtmlTable) reportPage4.getFirstByXPath("//table[@class='w3-table w3-bordered']");
        assertNotNull("Address table not found!", addressTable);

        HtmlTableRow lastRow = addressTable.getRows().get(addressTable.getRowCount() - 1);
        String addressId = lastRow.getCell(0).asText();

        HtmlTable salesTable = reportPage4.getFirstByXPath("(//table[@class='w3-table w3-bordered'])[2]");
        assertNotNull("Sales table not found!", salesTable);

        HtmlTableRow lastRow2 = salesTable.getRows().get(salesTable.getRowCount() - 1);
        String saleId = lastRow2.getCell(0).asText();

        HtmlForm form = reportPage4.getForms().get(0);
        HtmlInput addressIdInput = form.getInputByName("addr_id");
        addressIdInput.setValueAttribute(addressId);

        HtmlInput saleIdInput = form.getInputByName("sale_id");
        saleIdInput.setValueAttribute(saleId);

        HtmlInput submitButton5 = form.getInputByValue("Insert");
        HtmlPage reportPage5 = submitButton5.click();
        assertEquals("Sales Info", reportPage5.getTitleText());

        assertTrue(reportPage5.asText().contains(addressId));
        assertTrue(reportPage5.asText().contains(saleId));
    }
}
