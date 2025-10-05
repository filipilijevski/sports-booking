package com.ttclub.backend.booking.api;

import com.ttclub.backend.booking.dto.ProgramCardDto;
import com.ttclub.backend.booking.model.Program;
import com.ttclub.backend.booking.model.ProgramSlot;
import com.ttclub.backend.booking.repository.ProgramPackageRepository;
import com.ttclub.backend.booking.repository.ProgramRepository;
import com.ttclub.backend.booking.repository.ProgramSlotRepository;
import com.ttclub.backend.booking.service.BookingMapper;
import com.ttclub.backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/programs")
public class PublicProgramsController {

    private final ProgramRepository programs;
    private final ProgramPackageRepository packages;
    private final ProgramSlotRepository slots;
    private final UserRepository users;
    private final BookingMapper mapper = new BookingMapper();

    public PublicProgramsController(ProgramRepository programs,
                                    ProgramPackageRepository packages,
                                    ProgramSlotRepository slots,
                                    UserRepository users) {
        this.programs = programs;
        this.packages = packages;
        this.slots = slots;
        this.users = users;
    }

    @GetMapping
    public List<ProgramCardDto> listActive() {
        List<Program> list = programs.findAllByActiveTrueOrderByTitleAsc();
        return list.stream().map(p -> {
            var pk = packages.findByProgramIdOrderBySortOrderAscIdAsc(p.getId());
            var sl = slots.findByProgramIdOrderByWeekdayAscStartTimeAsc(p.getId());

            Set<Long> coachIds = sl.stream()
                    .map(ProgramSlot::getCoachId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));

            Map<Long, String> names = new HashMap<>();
            if (!coachIds.isEmpty()) {
                users.findNamesByIds(coachIds).forEach(v -> names.put(v.getId(), v.getName()));
            }

            return mapper.toCard(p, pk, sl, names);
        }).collect(Collectors.toList());
    }
}
