package com.cursovalido.backend.authentication.repository;
import com.cursovalido.backend.authentication.entity.ContaExcluida;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ContaExcluidaRepository extends JpaRepository<ContaExcluida, Long> {
 List<ContaExcluida> findByEmailHashInOrCpfHashIn(java.util.Collection<String> emails, java.util.Collection<String> cpfs);
}
