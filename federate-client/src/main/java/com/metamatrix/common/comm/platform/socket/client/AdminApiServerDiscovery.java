/*
 * JBoss, Home of Professional Open Source.
 * Copyright (C) 2008 Red Hat, Inc.
 * Copyright (C) 2000-2007 MetaMatrix, Inc.
 * Licensed to Red Hat, Inc. under one or more contributor 
 * license agreements.  See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package com.metamatrix.common.comm.platform.socket.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import com.metamatrix.admin.api.exception.AdminException;
import com.metamatrix.admin.api.objects.ProcessObject;
import com.metamatrix.admin.api.server.ServerAdmin;
import com.metamatrix.common.api.HostInfo;
import com.metamatrix.common.api.MMURL;

/**
 * Will discover hosts based upon an anon admin api call.
 * TODO: perform active polling
 */
public class AdminApiServerDiscovery extends UrlServerDiscovery {
	
	/**
	 * If the FIREWALL_HOST property is set, then this host name will be used instead of the process
	 * names returned by the AdminApi
	 */
	public static final String USE_URL_HOST = "AdminApiServerDiscovery.useUrlHost"; //$NON-NLS-1$
	
	private volatile List<HostInfo> knownHosts;
	private volatile boolean discoveredHosts;
	private boolean useUrlHost;
	
	@Override
	public List<HostInfo> getKnownHosts() {
		if (!discoveredHosts) {
			return super.getKnownHosts();
		}
		return knownHosts;
	}
	
	@Override
	public void init(MMURL url, Properties p) {
		super.init(url, p);
		useUrlHost = Boolean.valueOf(p.getProperty(USE_URL_HOST)).booleanValue();
	}
		
	@Override
	public void connectionSuccessful(HostInfo info, SocketServerInstance instance) {
		super.connectionSuccessful(info, instance);
		if (!discoveredHosts) {
			synchronized (this) {
				if (!discoveredHosts) {
					discoverHosts(info, instance);
				}
			}
		}
	}

	private void discoverHosts(HostInfo info, SocketServerInstance instance) {
		ServerAdmin serverAdmin = instance.getService(ServerAdmin.class);
		try {
			Collection<ProcessObject> processes = serverAdmin.getProcesses("*");
			this.knownHosts = new ArrayList<HostInfo>(processes.size());
			for (ProcessObject processObject : processes) {
				if (!processObject.isEnabled()) {
					continue;
				}
				if (useUrlHost) {
					this.knownHosts.add(new HostInfo(info.getHostName(), processObject.getPort()));
				} else {
					this.knownHosts.add(new HostInfo(processObject.getInetAddress().getHostName(), processObject.getPort(), processObject.getInetAddress()));
				}
			}
			discoveredHosts = true;
		} catch (AdminException e) {
			//ignore - will get an update on the next successful connection
		}
	}
	
	@Override
	public boolean isDynamic() {
		return true;
	}
}
