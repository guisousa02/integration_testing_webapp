

import static org.mockito.Mockito.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import webapp.services.ApplicationException;
import webapp.services.CustomerDTO;
import webapp.services.ICustomerService;
import webapp.webpresentation.AddCustomerPageController;

@RunWith(MockitoJUnitRunner.class)
public class AddCustomerPageControllerTest {
    
    @Mock
    private ICustomerService mockCustomerService;
    
    @Mock
    private HttpServletRequest mockRequest;
    
    @Mock
    private HttpServletResponse mockResponse;
    
    @Mock
    private RequestDispatcher mockRequestDispatcher;
    
    private AddCustomerPageController controller;
    
    @Before
    // public void setUp() {
    //     controller = new AddCustomerPageController(mockCustomerService);
    //     when(mockRequest.getRequestDispatcher(anyString())).thenReturn(mockRequestDispatcher);
    // }
    
    @Test
    public void testProcess_ValidCustomer() throws Exception {
        // Arrange
        String vat = "123456789";
        String designation = "Test Customer";
        String phone = "987654321";
        
        when(mockRequest.getParameter("vat")).thenReturn(vat);
        when(mockRequest.getParameter("designation")).thenReturn(designation);
        when(mockRequest.getParameter("phone")).thenReturn(phone);
        
        CustomerDTO expectedCustomer = new CustomerDTO(1, 123456789, designation, 987654321);
        when(mockCustomerService.getCustomerByVat(123456789)).thenReturn(expectedCustomer);
        
        // Act
        // controller.process(mockRequest, mockResponse);
        
        // Assert
        verify(mockCustomerService).addCustomer(123456789, designation, 987654321);
        verify(mockCustomerService).getCustomerByVat(123456789);
        verify(mockRequest).getRequestDispatcher("CustomerInfo.jsp");
        verify(mockRequestDispatcher).forward(mockRequest, mockResponse);
    }
} 