package org.votingsystem.serviceprovider.cdi;

import org.votingsystem.ejb.Config;
import org.votingsystem.model.voting.Election;
import org.votingsystem.qr.QRUtils;
import org.votingsystem.util.Messages;
import org.votingsystem.util.OperationType;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 *  License: https://github.com/votingsystem/votingsystem/wiki/Licencia
 */
@Named("electionsBean")
@SessionScoped
public class ElectionsBean implements Serializable {

    private static final Logger log = Logger.getLogger(ElectionsBean.class.getName());

    private static final Integer DEFAULT_PAGE_SIZE = 50;

    @PersistenceContext
    private EntityManager em;
    @Inject private Config config;
    private List<Election> electionList;
    private Integer pageSize;
    private Integer paginationPage = 1;
    private Integer totalPages;
    private Integer visiblePages = 7;
    private Election.State electionState;
    private String qrURL;
    private String pageCaption;


    public void loadElections() {
        String state = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest()).getParameter("state");
        try {
            electionState = Election.State.valueOf(state);
        } catch (Exception ex) {
            electionState = Election.State.ACTIVE;
        }
        qrURL = OperationType.GET_QR.getUrl(config.getEntityId()) + "?cht=qr&chs=180x180&chl=" +
                QRUtils.SYSTEM_ENTITY_KEY + "=" + config.getEntityId() + ";" +
                QRUtils.OPERATION_KEY + "=" + OperationType.FETCH_ELECTIONS.name() + ";";
        loadPages(DEFAULT_PAGE_SIZE, 0);
    }

    private void loadPages(Integer pageSize, Integer offset) {
        this.pageSize = pageSize;
        List<Election.State> inList = Arrays.asList(Election.State.ACTIVE);
        if(electionState == Election.State.TERMINATED) {
            inList = Arrays.asList(Election.State.TERMINATED, Election.State.CANCELED);
        } else
            inList = Arrays.asList(electionState);
        electionList = em.createQuery("select e from Election e where e.state in :inList")
                .setParameter("inList", inList).setParameter("inList", inList).setMaxResults(pageSize).setFirstResult(offset).getResultList();
        long totalCount = (long)em.createQuery("SELECT COUNT(e) FROM Election e where e.state in :inList")
                .setParameter("inList", inList).getSingleResult();
        this.totalPages = new Long(totalCount / pageSize).intValue() + 1;
        switch (electionState) {
            case ACTIVE:
                pageCaption = Messages.currentInstance().get("openElectionsLbl");
                break;
            case PENDING:
                pageCaption = Messages.currentInstance().get("pendingElectionsLbl");
                break;
            default:
                pageCaption = Messages.currentInstance().get("closedElectionsLbl");
                break;
        }
    }


    public void paginate() {
        loadPages(pageSize, ((paginationPage * pageSize) - pageSize));
    }

    public List<Election> getElectionList() {
        return electionList;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public Integer getVisiblePages() {
        return visiblePages;
    }

    public void setVisiblePages(Integer visiblePages) {
        this.visiblePages = visiblePages;
    }

    public Integer getPaginationPage() {
        return paginationPage;
    }

    public void setPaginationPage(Integer paginationPage) {
        this.paginationPage = paginationPage;
    }

    public Election.State getElectionState() {
        return electionState;
    }

    public String getPageColor() {
        switch (electionState) {
            case ACTIVE:
                return "#3a8287";
            case PENDING:
                return "orange";
            default:
                return "#ba0011";
        }
    }

    public String getQrURL() {
        return qrURL;
    }

    public void setQrURL(String qrURL) {
        this.qrURL = qrURL;
    }

    public String getPageCaption() {
        return pageCaption;
    }

    public void setPageCaption(String pageCaption) {
        this.pageCaption = pageCaption;
    }
}
