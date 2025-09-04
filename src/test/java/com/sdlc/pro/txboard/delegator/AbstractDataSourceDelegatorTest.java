package com.sdlc.pro.txboard.delegator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AbstractDataSourceDelegatorTest {
  private AbstractDataSourceDelegatorImplemTest abstractDataSourceDelegatorImplemTest;
  @Mock
  private DataSource dataSource;

  @BeforeEach
  void setUp() {
    abstractDataSourceDelegatorImplemTest = new AbstractDataSourceDelegatorImplemTest(dataSource);
  }

  @Test
  void should_delegate_get_connection_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.getConnection()).thenReturn(null);

      abstractDataSourceDelegatorImplemTest.getConnection();

      // Then
      Mockito.verify(dataSource).getConnection();
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_get_connection_with_username_password_to_data_source() {
    // Given
    String username = "username";
    String password = "password";

    // When
    try {
      when(dataSource.getConnection(any(), any())).thenReturn(null);

      abstractDataSourceDelegatorImplemTest.getConnection(username, password);

      // Then
      Mockito.verify(dataSource).getConnection(username, password);
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_get_log_writer_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.getLogWriter()).thenReturn(null);

      abstractDataSourceDelegatorImplemTest.getLogWriter();

      // Then
      Mockito.verify(dataSource).getLogWriter();
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_set_log_writer_to_data_source() {
    // Given

    // When
    try {
      Mockito.doNothing().when(dataSource).setLogWriter(any());

      abstractDataSourceDelegatorImplemTest.setLogWriter(null);

      // Then
      Mockito.verify(dataSource).setLogWriter(null);
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_set_login_timeout_to_data_source() {
    // Given

    // When
    try {
      Mockito.doNothing().when(dataSource).setLoginTimeout(anyInt());

      abstractDataSourceDelegatorImplemTest.setLoginTimeout(10);

      // Then
      Mockito.verify(dataSource).setLoginTimeout(10);
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_get_login_timeout_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.getLoginTimeout()).thenReturn(10);

      abstractDataSourceDelegatorImplemTest.getLoginTimeout();

      // Then
      Mockito.verify(dataSource).getLoginTimeout();
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_get_parent_logger_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.getParentLogger()).thenReturn(null);

      abstractDataSourceDelegatorImplemTest.getParentLogger();

      // Then
      Mockito.verify(dataSource).getParentLogger();
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_unwrap_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.unwrap(any())).thenReturn(true);

      abstractDataSourceDelegatorImplemTest.unwrap(Integer.class);

      // Then
      Mockito.verify(dataSource).unwrap(Integer.class);
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  @Test
  void should_delegate_is_wrapped_for_to_data_source() {
    // Given

    // When
    try {
      when(dataSource.isWrapperFor(any())).thenReturn(true);

      abstractDataSourceDelegatorImplemTest.isWrapperFor(Integer.class);

      // Then
      Mockito.verify(dataSource).isWrapperFor(Integer.class);
    } catch (SQLException e) {
      Assertions.fail();
    }
  }

  private static class AbstractDataSourceDelegatorImplemTest extends AbstractDataSourceDelegator {
    public AbstractDataSourceDelegatorImplemTest(DataSource dataSource) {
      super(dataSource);
    }
  }
}
