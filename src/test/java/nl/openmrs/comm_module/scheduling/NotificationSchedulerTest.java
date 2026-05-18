package nl.openmrs.comm_module.scheduling;

import nl.openmrs.comm_module.config.NotificationSchedulerProperties;
import nl.openmrs.comm_module.notification.DueNotificationProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSchedulerTest {

    @Mock
    private NotificationSchedulerProperties properties;

    @Mock
    private DueNotificationProcessor dueNotificationProcessor;

    @InjectMocks
    private NotificationScheduler notificationScheduler;

    @Test
    void roeptProcessorAanAlsSchedulerAanStaat() {
        when(properties.isEnabled()).thenReturn(true);
        notificationScheduler.checkDueNotifications();
        verify(dueNotificationProcessor).processDueNotifications();
    }

    @Test
    void slaatOverAlsSchedulerUitStaat() {
        when(properties.isEnabled()).thenReturn(false);
        notificationScheduler.checkDueNotifications();
        verify(dueNotificationProcessor, never()).processDueNotifications();
    }
}
