package com.sivacrackers.customer;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShopCustomerRepository extends JpaRepository<ShopCustomer, Long> {

    /** Name, phone or email contains the (escaped) search text; newest first. */
    @Query("""
            select c from ShopCustomer c
            where :pattern is null
               or c.fullName like :pattern escape '\\'
               or c.phone like :pattern escape '\\'
               or c.email like :pattern escape '\\'
            order by c.fullName asc, c.id asc""")
    List<ShopCustomer> search(@Param("pattern") String pattern, Pageable limit);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
