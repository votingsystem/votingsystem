<div id="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;">
                <div style="margin: 60px 0px 0px 0px;"><g:message code="sectionsLbl"/></div></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'app', action: 'index')}">
                    <g:message code="homeLbl"/> <i class="a fa fa-home" style="color: #fdd302"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'transaction', action: 'listener')}">
                    <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'groupVS', action: 'index')}" style="">
                    <g:message code="groupvsSectionLbl"/> <i class="fa fa-users"></i></i></a>
            </li>
            <li>
                <a href="${createLink(controller: 'app', action: 'tools')}">
                    <g:message code="toolsSectionLbl"/> <i class="fa fa-cogs"></i>
                </a>
            </li>
            <li>
                <a href="#">
                    <g:message code="subscriptionLbl"/><i class="fa fa-rss"></i>
                </a>
            </li>
            <li>
                <a  href="${createLink(controller: 'app', action: 'contact')}"
                    style="color:#f9f9f9; font-weight: bold;"><g:message code="contactLbl"/> <i class="fa fa-phone"></i>
                </a>
            </li>
        </ul>
    </nav>
</div>