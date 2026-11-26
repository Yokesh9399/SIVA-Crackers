package com.sivacrackers.settings;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findAllByActiveTrueOrderByDisplayOrderAscIdAsc();

    /** Every banner (active or not) in display order (admin). */
    List<Banner> findAllByOrderByDisplayOrderAscIdAsc();
}
