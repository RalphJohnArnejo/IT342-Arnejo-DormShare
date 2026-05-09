package edu.cit.arnejo.dormshare.expense;

import edu.cit.arnejo.dormshare.expense.dto.ExpenseRequest;
import edu.cit.arnejo.dormshare.group.GroupMembershipRepository;
import edu.cit.arnejo.dormshare.group.GroupService;
import edu.cit.arnejo.dormshare.group.entity.GroupMembershipEntity;
import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
import edu.cit.arnejo.dormshare.shared.entity.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private GroupService groupService;

    @MockBean
    private GroupMembershipRepository membershipRepository;

    @MockBean
    private UserRepository userRepository;

    private void setMockUser(UserEntity user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testGetSummary() throws Exception {
        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setRole("USER");
        setMockUser(mockUser);

        GroupMembershipEntity membership = new GroupMembershipEntity();
        membership.setUserId(1L);
        membership.setGroupId(1L);

        when(groupService.getUserGroupId(anyLong())).thenReturn(1L);
        when(membershipRepository.findByGroupId(anyLong())).thenReturn(Collections.singletonList(membership));
        when(expenseService.getSummary(anyLong(), anyLong())).thenReturn(ApiResponse.ok(null));

        mockMvc.perform(get("/api/expenses/summary"))
                .andExpect(status().isOk());
    }

    @Test
    public void testLogExpense() throws Exception {
        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setRole("USER");
        setMockUser(mockUser);

        GroupMembershipEntity membership = new GroupMembershipEntity();
        membership.setUserId(1L);
        membership.setGroupId(1L);

        when(groupService.getUserGroupId(anyLong())).thenReturn(1L);
        when(membershipRepository.findByGroupId(anyLong())).thenReturn(Collections.singletonList(membership));
        when(expenseService.createExpense(any(ExpenseRequest.class), anyLong()))
                .thenReturn(ApiResponse.ok(null));

        mockMvc.perform(post("/api/expenses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100.0, \"description\": \"Lunch\", \"splits\": []}"))
                .andExpect(status().isCreated());
    }
}
