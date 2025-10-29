package integration.service;

import integration.util.Utils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestService {

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void performConnectionLessTransaction() {
        // simulate task
        Utils.sleep(100);
    }
}
