package webapp.services;

public interface ICustomerService {
    CustomerDTO getCustomerByVat(int vat) throws ApplicationException;
    void addCustomer(int vat, String designation, int phoneNumber) throws ApplicationException;
    CustomersDTO getAllCustomers() throws ApplicationException;
    void addAddressToCustomer(int customerVat, String addr) throws ApplicationException;
    AddressesDTO getAllAddresses(int customerVat) throws ApplicationException;
    void updateCustomerPhone(int vat, int phoneNumber) throws ApplicationException;
    void removeCustomer(int vat) throws ApplicationException;
    boolean isValidVAT(int vat);
} 