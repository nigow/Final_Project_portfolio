package org.example.transaction.service;

import lombok.AllArgsConstructor;
import org.example.stockmarket.stocks.stock.entity.Stock;
import org.example.stockmarket.stocks.stock.service.StockService;
import org.example.transaction.exception.AccountBalanceException;
import org.example.transaction.exception.AccountInventoryException;
import org.example.transaction.model.entity.Account;
import org.example.transaction.model.entity.StockOwned;
import org.example.transaction.model.payload.BuyStockRequest;
import org.example.transaction.model.payload.SellStockRequest;
import org.example.transaction.repository.StockOwnedRepository;
import org.example.transaction.utils.ValidateStockTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StockOwnedService {

    @Autowired
    private final StockOwnedRepository stockOwnedRepository;
    @Autowired
    private final AccountService accountService;
    @Autowired
    private final StockService stockService;

    public void buyStock(BuyStockRequest buyStock) {
        Account account = accountService.getAccountByName(buyStock.getUsername());
        Stock stock = stockService.getStockByTickerSymbol(buyStock.getTicker());
        StockOwned stockOwned = findStockOwned(account, stock);
        if (!ValidateStockTransaction.doesAccountHaveEnoughMoney(account, buyStock, this.stockService)) {
            throw new AccountBalanceException("Account does not have funds for this purchase");
        }
        if (stockOwned != null) {
            //subtract transaction value from account balance
            accountService.updateBalanceAndSave(account, -1 * (buyStock.getSharesToBuy() * stock.getPrice()));
            stockOwned.updateCostBasisAndAmountOwned(buyStock.getSharesToBuy(), stock.getPrice());
            stockOwnedRepository.save(stockOwned);
            return;
        }
        accountService.updateBalanceAndSave(account, -1 * (buyStock.getSharesToBuy() * stock.getPrice()));
        saveNewStockOwned(buyStock, account, stock.getPrice());
    }

    public void saveNewStockOwned(BuyStockRequest buyStock, Account account, double stockPrice) {
        stockOwnedRepository.save(new StockOwned(
                account, buyStock.getTicker(), buyStock.getSharesToBuy(), stockPrice));
    }

    public void sellStock(SellStockRequest sellStock) {
        Account account = accountService.getAccountByName(sellStock.getUsername());
        if (!ValidateStockTransaction.doesAccountHaveEnoughStocks(account, sellStock)) {
            throw new AccountInventoryException("Account does not own enough stocks");
        }
        Stock stock = stockService.getStockByTickerSymbol(sellStock.getTicker());
        StockOwned stockOwned = findStockOwned(account, stock);
        account.updateTotalProfits(
                stockOwned.getCostBasis(),
                sellStock.getSharesToSell(),
                stock.getPrice());
        accountService.updateBalanceAndSave(account, stock.getPrice() * sellStock.getSharesToSell());
        if (sellStock.getSharesToSell() - stockOwned.getAmountOwned() == 0) {
            clearAndDeleteStockOwned(stockOwned);
        } else {
            stockOwned.setAmountOwned(stockOwned.getAmountOwned() - sellStock.getSharesToSell());
            stockOwnedRepository.save(stockOwned);
        }
    }

    public StockOwned findStockOwned(Account account, Stock stock) {
        return stockOwnedRepository.findAll().stream()
                .filter(stockOwned -> stockOwned.getTicker().equalsIgnoreCase(stock.getTicker()))
                .filter(stockOwned -> stockOwned.getAccount().getUsername().equals(account.getUsername()))
                .findFirst()
                .orElse(null);
    }

    public void clearAndDeleteStockOwned(StockOwned stockOwned) {
        stockOwnedRepository.delete(stockOwned);
    }
}
