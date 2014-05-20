<div id="menu" class="navBarMainMenu" style="">
    <nav>
        <h2><i class="fa fa-reorder"></i>
            <span style="text-decoration: underline; font-size: 1.2em;"><g:message code="sectionsLbl"/></span>
        </h2>
        <ul>
            <li>
                <a href="${createLink(controller: 'transaction', action: 'listener')}">
                    <g:message code="transactionsLbl"/> <i class="fa fa-money"></i>
                </a>
            </li>
            <li>
                <a href="${createLink(controller: 'groupVS')}" style="">
                    <g:message code="selectGroupvsLbl"/> <i class="fa fa-list"></i>
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

<g:include view="/include/dialog/windowAlertModal.gsp"/>