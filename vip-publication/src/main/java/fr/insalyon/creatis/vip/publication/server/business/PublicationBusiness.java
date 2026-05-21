package fr.insalyon.creatis.vip.publication.server.business;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.core.server.business.CoreUtil;
import fr.insalyon.creatis.vip.core.server.dao.DAOException;
import fr.insalyon.creatis.vip.publication.models.Publication;
import fr.insalyon.creatis.vip.publication.server.dao.PublicationDAO;

@Service
@Transactional
public class PublicationBusiness {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private PublicationDAO publicationDAO;

    public PublicationBusiness(PublicationDAO publicationDAO) {
        this.publicationDAO = publicationDAO;
    }


    /**
     *
     * @return @throws VipException
     */
    public List<Publication> getPublications()
            throws VipException {
        logger.debug("*******************PublicationBusiness getPublications*******************");
        try {
            return publicationDAO.getList();
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void removePublication(Long id, User currentUser)
            throws VipException {
        try {
            Publication existing = publicationDAO.getPublication(id);
            if (existing == null) {
                throw new VipException(DefaultError.NOT_FOUND, Publication.class.getSimpleName(), id.toString());
            }

            ensureAdminOrAuthor(currentUser, existing);
            publicationDAO.remove(id);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void addPublication(Publication pub, String vipAuthor)
            throws VipException {
        try {
            pub.setVipAuthor(vipAuthor);
            logger.info("Create publication request: title='{}', vipAuthor='{}', vipApplication='{}'",
                pub.getTitle(), pub.getVipAuthor(), pub.getVipApplication());
            assertDataIsOK(pub);
            publicationDAO.add(pub);
            logger.debug("Publication created: title='{}', vipAuthor='{}', vipApplication='{}'",
                    pub.getTitle(), pub.getVipAuthor(), pub.getVipApplication());
        } catch (DAOException ex) {
            throw new VipException(ex);
        } catch (VipException ex) {
            throw ex;
        }
    }

    public void updatePublication(Publication pub, User currentUser)
            throws VipException {
        try {
            Publication existing = publicationDAO.getPublication(pub.getId());
            if (existing == null) {
                throw new VipException(DefaultError.NOT_FOUND, Publication.class.getSimpleName(), pub.getId().toString());
            }

            ensureAdminOrAuthor(currentUser, existing);
            // Keep creator immutable to avoid ownership spoofing from request payload.
            pub.setVipAuthor(existing.getVipAuthor());
            assertDataIsOK(pub);
            publicationDAO.update(pub);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    private void assertDataIsOK(Publication publication) throws VipException {
        logger.debug("Publication payload values before validation: title='{}' (len={}), authors='{}' (len={}), date='{}', doi='{}', type='{}', typeName='{}', vipApplication='{}'",
                publication.getTitle(),
                publication.getTitle() == null ? -1 : publication.getTitle().length(),
                publication.getAuthors(),
                publication.getAuthors() == null ? -1 : publication.getAuthors().length(),
                publication.getDate(),
                publication.getDoi(),
                publication.getType(),
                publication.getTypeName(),
                publication.getVipApplication());

        assertRequiredText(publication.getTitle(), "title", "Title is required");
        assertRequiredText(publication.getDate(), "date", "Date is required");
        assertRequiredText(publication.getAuthors(), "authors", "Authors are required");
        if (publication.getDoi() != null && !publication.getDoi().trim().isEmpty()) {
            CoreUtil.assertOnlyLatin1Characters(publication.getDoi());
        }
        assertRequiredText(publication.getType(), "type", "Type is required");
        assertRequiredText(publication.getTypeName(), "typeName", "Type name is required");
        assertRequiredText(publication.getVipApplication(), "vipApplication", "VIP application is required");
        assertRequiredText(publication.getVipAuthor(), "vipAuthor", "VIP author is required");
        
    }

    private void assertRequiredText(String value, String fieldName, String message) throws VipException {
        if (value == null || value.trim().isEmpty()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, fieldName, message);
        }
        CoreUtil.assertOnlyLatin1Characters(value);
    }

    

    public Publication getPublication(Long id)
            throws VipException {
        try {
            return publicationDAO.getPublication(id);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    private void ensureAdminOrAuthor(User currentUser, Publication publication) throws VipException {
        if (currentUser == null) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }

        if (!currentUser.isSystemAdministrator() && !isAuthor(currentUser, publication)) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }
    }

    private boolean isAuthor(User currentUser, Publication publication) {
        return currentUser.getEmail() != null
                && publication.getVipAuthor() != null
                && currentUser.getEmail().equalsIgnoreCase(publication.getVipAuthor());
    }


}
