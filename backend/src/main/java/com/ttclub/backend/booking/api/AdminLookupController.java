package com.ttclub.backend.booking.api;

import com.ttclub.backend.model.RoleName;
import com.ttclub.backend.model.User;
import com.ttclub.backend.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/booking")
@PreAuthorize("hasAnyRole('OWNER','ADMIN')")
public class AdminLookupController {

    private final UserRepository users;

    public AdminLookupController(UserRepository users) {
        this.users = users;
    }

    public record CoachDto(Long id, String name, String email) { }

    @GetMapping("/coaches")
    public List<CoachDto> coaches() {
        // Pass RoleName enum to match repository signature
        List<User> coachList = users.findByRole_Name(RoleName.COACH);

        return coachList.stream()
                .map(u -> new CoachDto(
                        u.getId(),
                        buildDisplayName(u.getFirstName(), u.getLastName()),
                        u.getEmail()
                ))
                .toList();
    }

    private static String buildDisplayName(String first, String last) {
        String f = first == null ? "" : first.trim();
        String l = last  == null ? "" : last.trim();
        return (f + " " + l).trim();
    }
}
