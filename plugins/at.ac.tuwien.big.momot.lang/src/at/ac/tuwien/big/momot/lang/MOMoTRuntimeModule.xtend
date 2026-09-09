package at.ac.tuwien.big.momot.lang

import at.ac.tuwien.big.momot.lang.scoping.MOMoTScopeProvider
import com.google.inject.Binder
import org.eclipse.xtext.generator.IGenerator

class MOMoTRuntimeModule extends AbstractMOMoTRuntimeModule {
	override Class<? extends IGenerator> bindIGenerator() {
		return super.bindIGenerator()
	}

	override void configureIScopeProviderDelegate(Binder binder) {
		binder.bind(typeof(org.eclipse.xtext.scoping.IScopeProvider))
				.annotatedWith(com.google.inject.name.Names
						.named(org.eclipse.xtext.scoping.impl.AbstractDeclarativeScopeProvider.NAMED_DELEGATE))
				.to(typeof(MOMoTScopeProvider))
	}
}
