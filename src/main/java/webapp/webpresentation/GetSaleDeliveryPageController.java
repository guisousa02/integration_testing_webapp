package webapp.webpresentation;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import webapp.services.ApplicationException;
import webapp.services.SaleService;
import webapp.services.SalesDeliveryDTO;
import webapp.services.CustomerService;


@WebServlet("/GetSaleDeliveryPageController")
public class GetSaleDeliveryPageController extends PageController{
	private static final long serialVersionUID = 1L;

	@Override
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SaleService ss = SaleService.INSTANCE;        
		CustomerService cs = CustomerService.INSTANCE;

		SalesDeliveryHelper sdh = new SalesDeliveryHelper();
		request.setAttribute("salesDeliveryHelper", sdh);
		
		try {
			String vat = request.getParameter("vat");
			
			if (isInt(sdh, vat, "Invalid VAT number")) {
				int vatNumber = intValue(vat);
				
				// Validate VAT format
				if (!ss.isValidVAT(vatNumber)) {
					sdh.addMessage("Invalid VAT number format. VAT must be a 9-digit number starting with 1, 2, 5, 6, 8, or 9.");
					request.getRequestDispatcher("CustomerError.jsp").forward(request, response);
					return;
				}
				
				// Check if customer exists
				try {
					cs.getCustomerByVat(vatNumber);
				} catch (ApplicationException e) {
					sdh.addMessage("Customer with VAT number " + vatNumber + " does not exist.");
					request.getRequestDispatcher("CustomerError.jsp").forward(request, response);
					return;
				}
				
				SalesDeliveryDTO s = ss.getSalesDeliveryByVat(vatNumber);
				sdh.fillWithSalesDelivery(s.sales_delivery);
				request.getRequestDispatcher("ShowSalesDelivery.jsp").forward(request, response);
			}
		} catch (ApplicationException e) {
			sdh.addMessage("It was not possible to fulfill the request: " + e.getMessage());
			request.getRequestDispatcher("CustomerError.jsp").forward(request, response); 
		}
	}
}
