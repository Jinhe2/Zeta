package com.zeta.screen.terminal;

import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TerminalRepository extends JpaRepository<Terminal, Long> {

    List<Terminal> findByCabinetIdOrderByIdAsc(Long cabinetId);

    List<Terminal> findByTerminalStripIdOrderByIdAsc(Long terminalStripId);

    List<Terminal> findByIedDeviceIdOrderByIdAsc(Long iedDeviceId);

    /** 跨业务库组装响应时显式加载屏柜和端子排，避免离开 screen Session 后触发懒加载。 */
    @EntityGraph(attributePaths = {"cabinet", "terminalStrip", "iedDevice"})
    @Query("select t from Terminal t where t.id in :ids")
    List<Terminal> findAllWithCabinetAndStripByIdIn(@Param("ids") Collection<Long> ids);
}
