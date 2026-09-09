package at.ac.tuwien.big.momot.lang.scoping

import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider
import org.eclipse.xtext.xbase.scoping.batch.IBatchScopeProvider
import org.eclipse.xtext.xbase.scoping.batch.XbaseBatchScopeProvider
import org.eclipse.xtext.naming.QualifiedName
import org.eclipse.emf.ecore.resource.Resource
import org.eclipse.emf.ecore.EReference
import com.google.inject.Inject

class MOMoTScopeProvider extends XImportSectionNamespaceScopeProvider implements IBatchScopeProvider {
   public static final QualifiedName MOEA_FRAMEWORK = QualifiedName.create("org", "moeaframework");
   public static final QualifiedName MOEA = QualifiedName.create("at","ac", "tuwien", "big", "moea");
   public static final QualifiedName MOMOT = QualifiedName.create("at","ac", "tuwien", "big", "momot");
   
   @Inject
   XbaseBatchScopeProvider batchDelegate
   
   override protected getImplicitImports(boolean ignoreCase) {
      val imports = super.getImplicitImports(ignoreCase)
      imports.add(doCreateImportNormalizer(MOEA_FRAMEWORK, true, false))
      imports.add(doCreateImportNormalizer(MOEA, true, false))
      imports.add(doCreateImportNormalizer(MOMOT, true, false))
      return imports
   }

   override newSession(Resource resource) {
      return batchDelegate.newSession(resource)
   }
   
   override isBatchScopeable(EReference reference) {
      return batchDelegate.isBatchScopeable(reference)
   }
   
   override isConstructorCallScope(EReference reference) {
      return batchDelegate.isConstructorCallScope(reference)
   }
   
   override isFeatureCallScope(EReference reference) {
      return batchDelegate.isFeatureCallScope(reference)
   }
}
