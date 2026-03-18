package iocode.web.app.repository;

import iocode.web.app.entity.Transactions;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transactions,String> {

}
