import org.votingsystem.cooin.util.ApplicationContextHolder

// Place your Spring DSL code here
beans = {
    votingSystemApplicationContex(ApplicationContextHolder) { bean ->
        bean.factoryMethod = 'getInstance'
    }

}
