package fr.insalyon.creatis.vip.publication.server.controller;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.insalyon.creatis.vip.core.client.DefaultError;
import fr.insalyon.creatis.vip.core.client.VipException;
import fr.insalyon.creatis.vip.core.models.User;
import fr.insalyon.creatis.vip.publication.models.Publication;
import fr.insalyon.creatis.vip.publication.server.business.PublicationBusiness;

@RestController
@RequestMapping("/publications")
public class PublicationController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final PublicationBusiness publicationBusiness;
    private final Supplier<User> userProvider;

    @Autowired
    public PublicationController(PublicationBusiness publicationBusiness, Supplier<User> userProvider) {
        this.publicationBusiness = publicationBusiness;
        this.userProvider = userProvider;
    }

    @GetMapping
    public List<Publication> list() throws VipException {
        return publicationBusiness.getPublications();
    }

    @GetMapping("{id}")
    public Publication get(@PathVariable Long id) throws VipException {
        Publication publication = publicationBusiness.getPublication(id);

        if (publication == null) {
            throw new VipException(DefaultError.NOT_FOUND, Publication.class.getSimpleName(), id.toString());
        }
        return publication;
    }

    @PostMapping
    public void create(@RequestBody Publication publication) throws VipException {
        if (publication == null) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "publication", "Publication payload is required");
        }

        User currentUser = userProvider.get();
        publication.setVipAuthor(currentUser.getEmail());
        logger.info("Create publication request: title='{}', vipAuthor='{}', vipApplication='{}'",
                publication.getTitle(), publication.getVipAuthor(), publication.getVipApplication());
        publicationBusiness.addPublication(publication);
    }

    @PutMapping("{id}")
    public void update(@PathVariable Long id, @RequestBody Publication publication) throws VipException {
        ensureAdmin();

        if (publication == null) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "publication", "Publication payload is required");
        }

        if (publication.getId() == null || !id.equals(publication.getId())) {
            throw new VipException(DefaultError.BAD_INPUT_FIELD, "id", "Publication id do not match!");
        }

        logger.info("Update publication request: id='{}', title='{}', vipApplication='{}'",
            id, publication.getTitle(), publication.getVipApplication());

        Publication existing = publicationBusiness.getPublication(id);
        if (existing == null) {
            throw new VipException(DefaultError.NOT_FOUND, Publication.class.getSimpleName(), id.toString());
        }

        publicationBusiness.updatePublication(publication);
    }

    @DeleteMapping("{id}")
    public void delete(@PathVariable Long id) throws VipException {
        ensureAdmin();

        Publication existing = publicationBusiness.getPublication(id);
        if (existing == null) {
            throw new VipException(DefaultError.NOT_FOUND, Publication.class.getSimpleName(), id.toString());
        }

        publicationBusiness.removePublication(id);
    }

    private void ensureAdmin() throws VipException {
        User currentUser = userProvider.get();
        if (!currentUser.isSystemAdministrator()) {
            throw new VipException(DefaultError.ACCESS_DENIED);
        }
    }
}
