package at.ac.tuwien.big.momot.lang

class MOMoTStandaloneSetup extends MOMoTStandaloneSetupGenerated {
	def static void doSetup() {
		new MOMoTStandaloneSetup().createInjectorAndDoEMFRegistration()
	}
}
