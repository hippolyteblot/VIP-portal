package fr.insalyon.creatis.vip.publication.server.business;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
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

    public void removePublication(Long id)
            throws VipException {
        try {
            publicationDAO.remove(id);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }

    public void addPublication(Publication pub)
            throws VipException {
        try {
            assertDataIsOK(pub);
            publicationDAO.add(pub);
            logger.debug("Publication created: title='{}', vipAuthor='{}', vipApplication='{}'",
                    pub.getTitle(), pub.getVipAuthor(), pub.getVipApplication());
        } catch (DAOException ex) {
            logger.error("Publication persistence failed on create: title='{}', vipAuthor='{}', vipApplication='{}'",
                pub.getTitle(), pub.getVipAuthor(), pub.getVipApplication(), ex);
            throw new VipException(ex);
        } catch (VipException ex) {
            logger.warn("Publication validation failed on create: title='{}', vipAuthor='{}', vipApplication='{}', cause='{}'",
                    pub.getTitle(), pub.getVipAuthor(), pub.getVipApplication(), ex.getMessage());
            throw ex;
        }
    }

    public void updatePublication(Publication pub)
            throws VipException {
        try {
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

        if (publication.getTitle() == null || publication.getTitle().trim().isEmpty()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "title", "Title is required");
        } else {
            CoreUtil.assertOnlyLatin1Characters(publication.getTitle());
        }
        if (publication.getAuthors() == null || publication.getAuthors().trim().isEmpty()) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "authors", "Authors are required");
        } else {
            CoreUtil.assertOnlyLatin1Characters(publication.getAuthors());
        }
        if (publication.getDoi() != null && !publication.getDoi().trim().isEmpty()) {
            CoreUtil.assertOnlyLatin1Characters(publication.getDoi());
        }
        
    }

    

    public Publication getPublication(Long id)
            throws VipException {
        try {
            return publicationDAO.getPublication(id);
        } catch (DAOException ex) {
            throw new VipException(ex);
        }
    }


}
