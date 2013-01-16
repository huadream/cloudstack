// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.api.command.user.vpn;

import org.apache.cloudstack.api.*;
import org.apache.cloudstack.api.response.Site2SiteCustomerGatewayResponse;
import org.apache.cloudstack.api.response.Site2SiteVpnGatewayResponse;
import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.response.Site2SiteVpnConnectionResponse;
import com.cloud.event.EventTypes;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Site2SiteVpnConnection;
import com.cloud.network.Site2SiteVpnGateway;
import com.cloud.network.vpc.Vpc;

@APICommand(name = "createVpnConnection", description="Create site to site vpn connection", responseObject=Site2SiteVpnConnectionResponse.class)
public class CreateVpnConnectionCmd extends BaseAsyncCreateCmd {
    public static final Logger s_logger = Logger.getLogger(CreateVpnConnectionCmd.class.getName());

    private static final String s_name = "createvpnconnectionresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name=ApiConstants.S2S_VPN_GATEWAY_ID, type=CommandType.UUID, entityType=Site2SiteVpnGatewayResponse.class,
            required=true, description="id of the vpn gateway")
    private Long vpnGatewayId;

    @Parameter(name=ApiConstants.S2S_CUSTOMER_GATEWAY_ID, type=CommandType.UUID, entityType=Site2SiteCustomerGatewayResponse.class,
            required=true, description="id of the customer gateway")
    private Long customerGatewayId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getVpnGatewayId() {
        return vpnGatewayId;
    }

    public Long getCustomerGatewayId() {
        return customerGatewayId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////


    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Vpc vpc = _vpcService.getVpc(getVpnGateway().getVpcId());
        return vpc.getAccountId();
    }

    @Override
    public String getEventDescription() {
        return "Create site-to-site VPN connection for account " + getEntityOwnerId();
    }

    @Override
    public String getEventType() {
        return EventTypes.EVENT_S2S_VPN_CONNECTION_CREATE;
    }

    @Override
    public void create() {
        try {
            Site2SiteVpnConnection conn = _s2sVpnService.createVpnConnection(this);
            if (conn != null) {
                this.setEntityId(conn.getId());
                this.setEntityUuid(conn.getUuid());
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create site to site vpn connection");
            }
        } catch (NetworkRuleConflictException e) {
            s_logger.info("Network rule conflict: " + e.getMessage());
            s_logger.trace("Network Rule Conflict: ", e);
            throw new ServerApiException(BaseCmd.NETWORK_RULE_CONFLICT_ERROR, e.getMessage());
        }
    }

    @Override
    public void execute(){
        try {
            Site2SiteVpnConnection result = _s2sVpnService.startVpnConnection(this.getEntityId());
            if (result != null) {
                Site2SiteVpnConnectionResponse response = _responseGenerator.createSite2SiteVpnConnectionResponse(result);
                response.setResponseName(getCommandName());
                this.setResponseObject(response);
            } else {
                throw new ServerApiException(BaseCmd.INTERNAL_ERROR, "Failed to create site to site vpn connection");
            }
        } catch (ResourceUnavailableException ex) {
            s_logger.warn("Exception: ", ex);
            throw new ServerApiException(BaseCmd.RESOURCE_UNAVAILABLE_ERROR, ex.getMessage());
        }
    }


    @Override
    public String getSyncObjType() {
        return BaseAsyncCmd.vpcSyncObject;
    }

    @Override
    public Long getSyncObjId() {
        return getVpnGateway().getVpcId();
    }

    private Site2SiteVpnGateway getVpnGateway() {
        return _s2sVpnService.getVpnGateway(vpnGatewayId);
    }
}
