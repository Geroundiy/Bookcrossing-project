package com.bookcrossing.controller;

import com.bookcrossing.model.Complaint;
import com.bookcrossing.model.User;
import com.bookcrossing.repository.ComplaintRepository;
import com.bookcrossing.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ComplaintController")
class ComplaintControllerTest {

    @Mock ComplaintRepository complaintRepository;
    @Mock UserService userService;
    @InjectMocks ComplaintController complaintController;

    private Principal principal;
    private RedirectAttributes ra;
    private User author;

    @BeforeEach
    void setUp() {
        principal = () -> "alice";
        ra        = mock(RedirectAttributes.class);
        author    = new User(); author.setId(1L); author.setUsername("alice");
    }

    @Test
    @DisplayName("POST /complaints/submit — сохраняет жалобу и редиректит на /")
    void submitComplaint_savesAndRedirects() {
        when(userService.findByUsername("alice")).thenReturn(author);

        String view = complaintController.submitComplaint(
                10L, "Книга-тест", "SPAM", "Спам-описание", principal, ra);

        assertThat(view).isEqualTo("redirect:/");
        verify(ra).addFlashAttribute(eq("success"), any());

        ArgumentCaptor<Complaint> cap = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository).save(cap.capture());

        Complaint saved = cap.getValue();
        assertThat(saved.getAuthor()).isEqualTo(author);
        assertThat(saved.getTargetBookId()).isEqualTo(10L);
        assertThat(saved.getTargetBookTitle()).isEqualTo("Книга-тест");
        assertThat(saved.getType()).isEqualTo(Complaint.ComplaintType.SPAM);
        assertThat(saved.getDescription()).isEqualTo("Спам-описание");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Тип FAKE — жалоба сохраняется с типом FAKE")
    void submitFakeComplaint_correctType() {
        when(userService.findByUsername("alice")).thenReturn(author);

        complaintController.submitComplaint(
                5L, "Фейк", "FAKE", "Это фейк", principal, ra);

        ArgumentCaptor<Complaint> cap = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(Complaint.ComplaintType.FAKE);
    }

    @Test
    @DisplayName("Тип OTHER — жалоба сохраняется с типом OTHER")
    void submitOtherComplaint_correctType() {
        when(userService.findByUsername("alice")).thenReturn(author);

        complaintController.submitComplaint(
                7L, "Другое", "OTHER", "Произвольная причина", principal, ra);

        ArgumentCaptor<Complaint> cap = ArgumentCaptor.forClass(Complaint.class);
        verify(complaintRepository).save(cap.capture());
        assertThat(cap.getValue().getType()).isEqualTo(Complaint.ComplaintType.OTHER);
    }
}