/*
 * Copyright 2015-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.completion;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.dsl.CheckPointedParseException;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;

/**
 * Provides completions for the case where the user has started to type an app
 * configuration property name but it is not typed in full yet.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class UnfinishedConfigurationPropertyNameRecoveryStrategy
		extends StacktraceFingerprintingRecoveryStrategy<CheckPointedParseException> {

	private final ProposalsCollectorSupportUtils collectorSupport;

	UnfinishedConfigurationPropertyNameRecoveryStrategy(AppRegistry appRegistry,
			ApplicationConfigurationMetadataResolver metadataResolver) {
		super(CheckPointedParseException.class, "file --foo", "file | bar --quick", "file --foo.",
				"file | bar " + "--quick.");
		this.collectorSupport = new ProposalsCollectorSupportUtils(appRegistry, metadataResolver);
	}

	@Override
	public void addProposals(String dsl, CheckPointedParseException exception, int detailLevel, List<CompletionProposal> collector) {
		String safe = exception.getExpressionStringUntilCheckpoint();

		StreamDefinition streamDefinition = new StreamDefinition("__dummy", safe);
		StreamAppDefinition lastApp = streamDefinition.getDeploymentOrderIterator().next();

		AppRegistration appRegistration = this.collectorSupport.findAppRegistration(lastApp.getName(), CompletionUtils.determinePotentialTypes(lastApp));
		if (appRegistration != null) {
			String startsWith = ProposalsCollectorSupportUtils.computeStartsWith(exception);
			Set<String> alreadyPresentOptions = new HashSet<>(lastApp.getProperties().keySet());
			this.collectorSupport.addPropertiesProposals(safe, startsWith, appRegistration, alreadyPresentOptions, collector, detailLevel);
		}
	}
}
