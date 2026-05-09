package edu.cit.arnejo.dormshare.pantry;

import edu.cit.arnejo.dormshare.group.GroupService;
import edu.cit.arnejo.dormshare.pantry.dto.PantryItemRequest;
import edu.cit.arnejo.dormshare.shared.dto.ApiResponse;
import edu.cit.arnejo.dormshare.shared.entity.UserEntity;
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
public class PantryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PantryService pantryService;

    @MockBean
    private GroupService groupService;

    private void setMockUser(UserEntity user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                user, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    public void testGetAllItems() throws Exception {
        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setRole("USER");
        setMockUser(mockUser);

        when(groupService.getVerifiedGroupId(anyLong(), any())).thenReturn(1L);
        when(pantryService.getAllItems(anyLong())).thenReturn(ApiResponse.ok(null));

        mockMvc.perform(get("/api/pantry"))
                .andExpect(status().isOk());
    }

    @Test
    public void testAddItem() throws Exception {
        UserEntity mockUser = new UserEntity();
        mockUser.setId(1L);
        mockUser.setRole("USER");
        setMockUser(mockUser);

        when(groupService.getUserGroupId(anyLong())).thenReturn(1L);
        when(pantryService.addItem(any(PantryItemRequest.class), anyLong(), anyLong()))
                .thenReturn(ApiResponse.ok(null));

        mockMvc.perform(post("/api/pantry")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"itemName\": \"Milk\", \"status\": \"IN\"}"))
                .andExpect(status().isCreated());
    }
}
