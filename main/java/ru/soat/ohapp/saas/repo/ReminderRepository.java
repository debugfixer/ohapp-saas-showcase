package ru.soat.ohapp.saas.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.soat.ohapp.saas.domain.Reminder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    @Query("""
           select r from Reminder r
           where r.status in (ru.soat.ohapp.saas.domain.Reminder$Status.PENDING, 
                              ru.soat.ohapp.saas.domain.Reminder$Status.FAILED)
             and r.nextRunAt is not null
             and r.nextRunAt <= :now
           order by r.nextRunAt asc
           """)
    List<Reminder> findDue(Instant now);
}
