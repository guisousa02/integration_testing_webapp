package webapp.webpresentation;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import webapp.services.ApplicationException;
import webapp.services.SaleService;
import webapp.services.SalesDTO;
import webapp.services.CustomerService;

@WebServlet("/AddSalePageController")
public class AddSalePageController extends PageController{
	private static final long serialVersionUID = 1L;

	@Override
	protected void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		SaleService ss = SaleService.INSTANCE;
		CustomerService cs = CustomerService.INSTANCE;
	
		SalesHelper sh = new SalesHelper();
		request.setAttribute("salesHelper", sh);
		
		try{
			String vat = request.getParameter("customerVat");
			
			if (isInt(sh, vat, "Invalid VAT number")) {
				int vatNumber = intValue(vat);
				if (!ss.isValidVAT(vatNumber)) {
					sh.addMessage("Invalid VAT number format. VAT must be a 9-digit number starting with 1, 2, 5, 6, 8, or 9.");
					request.getRequestDispatcher("CustomerError.jsp").forward(request, response);
					return;
				}
				try {
					cs.getCustomerByVat(vatNumber);
				} catch (ApplicationException e) {
					sh.addMessage("Customer with VAT number " + vatNumber + " does not exist.");
					request.getRequestDispatcher("CustomerError.jsp").forward(request, response);
					return;
				}
				
				ss.addSale(vatNumber);
				SalesDTO s = ss.getSaleByCustomerVat(vatNumber);
				sh.fillWithSales(s.sales);
				request.getRequestDispatcher("SalesInfo.jsp").forward(request, response);
			}
		} catch (ApplicationException e) {
			sh.addMessage("It was not possible to fulfill the request: " + e.getMessage());
			request.getRequestDispatcher("CustomerError.jsp").forward(request, response); 
		}
	}
}
