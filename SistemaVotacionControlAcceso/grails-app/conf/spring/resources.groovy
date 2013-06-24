// Place your Spring DSL code here
beans = {

	votingSystemApplicationContex(org.sistemavotacion.utils.VotingSystemApplicationContex) { bean ->
		bean.factoryMethod = 'getInstance'
	}
	
	
	//to get ApplicationEventPublisher
	//publishService(org.sistemavotacion.controlacceso.PublishService) {}
}
