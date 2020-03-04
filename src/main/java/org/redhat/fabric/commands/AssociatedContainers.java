/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redhat.fabric.commands;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.service.command.Function;

import io.fabric8.api.FabricService;
import io.fabric8.api.ZooKeeperClusterService;
import io.fabric8.api.scr.ValidatingReference;
import io.fabric8.boot.commands.support.AbstractCommandComponent;
import io.fabric8.boot.commands.support.ProfileCompleter;
import io.fabric8.boot.commands.support.ResolverCompleter;
import io.fabric8.boot.commands.support.VersionCompleter;
import io.fabric8.commands.support.RootContainerCompleter;

@Component(immediate = true)
@Service({ Function.class, AbstractCommand.class })
@org.apache.felix.scr.annotations.Properties({
		@Property(name = "osgi.command.scope", value = AssociatedContainers.SCOPE_VALUE),
		@Property(name = "osgi.command.function", value = AssociatedContainers.FUNCTION_VALUE) })
public class AssociatedContainers extends AbstractCommandComponent {

	public static final String SCOPE_VALUE = "fabric";
	public static final String FUNCTION_VALUE = "container-profile-synch";
	public static final String DESCRIPTION = "Displays all profiles with associated containers and bundles list , provide an argument for a specific profile";
	@Reference(referenceInterface = FabricService.class)
	private final ValidatingReference<FabricService> fabricService = new ValidatingReference<FabricService>();
	
	@Reference(referenceInterface = ZooKeeperClusterService.class)
    private final ValidatingReference<ZooKeeperClusterService> clusterService = new ValidatingReference<ZooKeeperClusterService>();

	// Optional Completers
    @Reference(referenceInterface = ProfileCompleter.class, bind = "bindProfileCompleter", unbind = "unbindProfileCompleter")
    private ProfileCompleter profileCompleter; // dummy field
    @Reference(referenceInterface = ResolverCompleter.class, bind = "bindResolverCompleter", unbind = "unbindResolverCompleter")
    private ResolverCompleter resolverCompleter;
    @Reference(referenceInterface = VersionCompleter.class, bind = "bindVersionCompleter", unbind = "unbindVersionCompleter")
    private VersionCompleter versionCompleter; // dummy field
 

	
	@Activate
	void activate() {
		activateComponent();
	}

	@Deactivate
	void deactivate() {
		deactivateComponent();
	}


	@Override
	public Action createNewAction() {
		assertValid();
		return new AssociatedContainersAction(fabricService.get(),clusterService.get());
	}
	

    void bindFabricService(FabricService fabricService) {
        this.fabricService.bind(fabricService);
    }

    void unbindFabricService(FabricService fabricService) {
        this.fabricService.unbind(fabricService);
    }
    
    void bindClusterService(ZooKeeperClusterService clusterService) {
        this.clusterService.bind(clusterService);
    }

    void unbindClusterService(ZooKeeperClusterService clusterService) {
        this.clusterService.unbind(clusterService);
    }
    
    void bindRootContainerCompleter(RootContainerCompleter completer) {
        bindCompleter(completer);
    }

    void unbindRootContainerCompleter(RootContainerCompleter completer) {
        unbindCompleter(completer);
    }


    void bindResolverCompleter(ResolverCompleter completer) {
        bindOptionalCompleter(completer);
    }

    void unbindResolverCompleter(ResolverCompleter completer) {
        unbindOptionalCompleter(completer);
    }

  
   
    void bindProfileCompleter(ProfileCompleter completer) {
        bindCompleter(completer);
    }

    void unbindProfileCompleter(ProfileCompleter completer) {
        unbindCompleter(completer);
    }

    void bindVersionCompleter(VersionCompleter completer) {
        bindCompleter(completer);
    }

    void unbindVersionCompleter(VersionCompleter completer) {
        unbindCompleter(completer);
    }

}
