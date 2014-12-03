package org.redpill.alfresco.webscripts.connector;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.extensions.surf.FrameworkUtil;
import org.springframework.extensions.surf.RequestContext;
import org.springframework.extensions.surf.exception.AuthenticationException;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.connector.AlfrescoAuthenticator;
import org.springframework.extensions.webscripts.connector.Connector;
import org.springframework.extensions.webscripts.connector.ConnectorContext;
import org.springframework.extensions.webscripts.connector.ConnectorSession;
import org.springframework.extensions.webscripts.connector.Credentials;
import org.springframework.extensions.webscripts.connector.Response;

public class AlfrescoTicketAuthenticator extends AlfrescoAuthenticator {

  @Override
  public ConnectorSession authenticate(String endpoint, Credentials credentials, ConnectorSession connectorSession) throws AuthenticationException {
    RequestContext request = FrameworkUtil.getCurrentRequestContext();

    String ticket = request.getParameter("alfTicket");

    if (isTicketValid(connectorSession, ticket)) {
      // place the ticket back into the connector session
      if (connectorSession != null) {
        connectorSession.setParameter(CS_PARAM_ALF_TICKET, ticket);

        // signal that this succeeded
        return connectorSession;
      }
    }

    return super.authenticate(endpoint, credentials, connectorSession);
  }

  private boolean isTicketValid(ConnectorSession connectorSession, String ticket) {
    if (StringUtils.isBlank(ticket)) {
      return false;
    }

    if (StringUtils.isNotBlank(connectorSession.getParameter("ALWAYS_VALID"))) {
      return true;
    }

    try {
      Connector connector = FrameworkUtil.getConnector("alfresco");

      ConnectorSession fakeSession = clone(connectorSession);

      fakeSession.setParameter(CS_PARAM_ALF_TICKET, ticket);
      fakeSession.setParameter("ALWAYS_VALID", "true");
      connector.setConnectorSession(fakeSession);

      Map<String, String> parameters = new HashMap<String, String>();
      parameters.put("alf_ticket", ticket);
      
      Map<String, String> headers = Collections.emptyMap();
      ConnectorContext context = new ConnectorContext(parameters, headers);
      
      Response response = connector.call("/api/metadata", context);

      return response.getStatus().getCode() == Status.STATUS_OK;
    } catch (Exception ex) {
      return false;
    }
  }

  public ConnectorSession clone(ConnectorSession connectorSession) {
    ByteArrayOutputStream baos = null;
    ObjectOutputStream oos = null;
    ByteArrayInputStream bais = null;
    ObjectInputStream ois = null;

    try {
      baos = new ByteArrayOutputStream();
      oos = new ObjectOutputStream(baos);
      oos.writeObject(connectorSession);

      bais = new ByteArrayInputStream(baos.toByteArray());
      ois = new ObjectInputStream(bais);
      return (ConnectorSession) ois.readObject();
    } catch (IOException e) {
      return null;
    } catch (ClassNotFoundException e) {
      return null;
    } finally {
      IOUtils.closeQuietly(baos);
      IOUtils.closeQuietly(oos);
      IOUtils.closeQuietly(bais);
      IOUtils.closeQuietly(ois);
    }
  }
}
