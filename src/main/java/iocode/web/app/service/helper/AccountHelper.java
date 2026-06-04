package iocode.web.app.service.helper;


import iocode.web.app.dto.AccountDto;
import iocode.web.app.dto.ConvertDto;
import iocode.web.app.entity.*;
import iocode.web.app.repository.AccountRepository;
import iocode.web.app.repository.TransactionRepository;
import iocode.web.app.service.ExchangeRateService;
import iocode.web.app.util.RandomUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.naming.OperationNotSupportedException;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Getter
public class AccountHelper {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;

    private final Map<String,String> CURRENCIES = Map.of(
            "USD", "United States Dollar",
            "EUR","Euro",
            "GBP","British Pound",
            "JPY","Japanese Yen",
            "NGN","Nigerian Naira",
            "INR","Indian Rupee"
    );

    public Account createAccount(AccountDto accountDto, User user) throws Exception {
        System.out.println("AccountHelper: createAccount");
        long accountNumber;

        validateAccountNonExistsForUser(accountDto.getCode(), user.getUid());

        do {
            accountNumber = new RandomUtil().generateRandom(10);
        }while (accountRepository.existsByAccountNumber(accountNumber));

        var account =  Account.builder()
                .accountNumber(accountNumber)
                .accountName(user.getFirstname()+" "+user.getLastname())
                .balance(1000)
                .owner(user)
                .code(accountDto.getCode())
                .symbol(accountDto.getSymbol())
                .label(getCURRENCIES().get(accountDto.getCode()))
                .build();
        return accountRepository.save(account);
    }

    public Transaction performTransfer(Account senderAccount, Account receiverAccount, double amount, User user) throws Exception {
        // Implementation of transfer logic goes here

        System.out.println("AccountHelper: performTransfer");

        validateSufficientFunds(senderAccount,(amount*1.01));
        senderAccount.setBalance(senderAccount.getBalance() - amount*1.01);
        receiverAccount.setBalance(receiverAccount.getBalance() + amount);
        accountRepository.saveAll(List.of(senderAccount,receiverAccount));

        var senderTransaction = Transaction.builder()
                .account(senderAccount)
                .status(Status.COMPLETED)
                .type(Type.WITHDRAW)
                .txFee(amount*1.01)
                .amount(amount)
                .owner(senderAccount.getOwner())
                .build();

        var recipientTransaction = Transaction.builder()
                .account(senderAccount)
                .status(Status.COMPLETED)
                .type(Type.CREDIT)
                .amount(amount)
                .owner(receiverAccount.getOwner())
                .build();

        return transactionRepository.saveAll(List.of(senderTransaction,recipientTransaction)).getFirst();
    }

    public void validateAccountOwner(Account account,User user) throws OperationNotSupportedException{
        System.out.println("AccountHelper: validateAccountOwner");
        if(!account.getOwner().getUid().equals(user.getUid())){
            throw new OperationNotSupportedException("Invalid account owner");
        }
    }

    public void validateAccountNonExistsForUser(String code, String uid) throws Exception{
        System.out.println("AccountHelper: validateAccountNonExistsForUser");
        if(accountRepository.existsByCodeAndOwnerUid(code,uid)) {
            throw new Exception("Account of this type already exist for this user");
        }
    }

    public void validateSufficientFunds(Account account, double amount) throws Exception {
        System.out.println("AccountHelper: validateSufficientFunds");
        if(account.getBalance()<0){
            throw new OperationNotSupportedException("Insufficient fund in account");
        }
    }

    public void validateAmount(double amount) throws Exception{
        System.out.println("AccountHelper: validateAmount");
        if(amount<=0){
            throw new IllegalArgumentException("Invalid Amount");
        }
    }

    public void validateDifferentCurrencyType(ConvertDto convertDto) throws Exception{
        System.out.println("AccountHelper: validateDifferentCurrencyType");
        if (convertDto.getToCurrency().equals(convertDto.getFromCurrency())){
            throw new IllegalArgumentException("Conversion between same currency type is not allowed.");
        }
    }

    public void validateAccountOwnerShip(ConvertDto convertDto, String uid) throws Exception{
        System.out.println("AccountHelper: validateAccountOwnerShip");
        accountRepository.findByCodeAndOwnerUid(convertDto.getFromCurrency(),uid).orElseThrow();
        accountRepository.findByCodeAndOwnerUid(convertDto.getToCurrency(),uid).orElseThrow();
    }

    public void validateConversion(ConvertDto convertDto, String uid) throws Exception {
        System.out.println("AccountHelper: validateConversion");
        validateDifferentCurrencyType(convertDto);
        validateAccountOwnerShip(convertDto,uid);
        validateAmount(convertDto.getAmount());
        validateSufficientFunds(accountRepository.findByCodeAndOwnerUid(convertDto.getFromCurrency(),uid).get(),
                convertDto.getAmount());
    }

    public Transaction convertCurrency(ConvertDto convertDto, User user)throws Exception{
        System.out.println("AccountHelper: convertCurrency");
        validateConversion(convertDto,user.getUid());
        var rates = exchangeRateService.getRates();
        var sendingRates = rates.get(convertDto.getFromCurrency());
        var receivingRates = rates.get(convertDto.getToCurrency());
        var computedAmount = (receivingRates/sendingRates) * convertDto.getAmount();


        var fromAccount = accountRepository.findByCodeAndOwnerUid(convertDto.getFromCurrency(), user.getUid()).get();
        var toAccount = accountRepository.findByCodeAndOwnerUid(convertDto.getToCurrency(),user.getUid()).get();

        fromAccount.setBalance(fromAccount.getBalance() - (convertDto.getAmount() * 1.01));
        toAccount.setBalance(toAccount.getBalance() + computedAmount);
        accountRepository.saveAll(List.of(fromAccount,toAccount));

        var transaction  = Transaction.builder()

                .owner(user)
                .amount(convertDto.getAmount())
                .txFee(convertDto.getAmount() * 0.01)
                .account(fromAccount)
                .status(Status.COMPLETED)
                .type(Type.CONVERSION)
                .build();

        return transactionRepository.save(transaction);
    }
}
