package me.zoemartin.rubie.modules.gatekeeper;

import io.javalin.Javalin;
import io.javalin.http.Context;
import me.zoemartin.rubie.core.util.DatabaseUtil;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.criteria.*;
import java.util.List;
import java.util.UUID;

public class WebSocket {
    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    static void startAPI() {
        Javalin app = Javalin.create(config -> config.showJavalinBanner = false).start(7000);
        app.post("/gatekeeper", WebSocket::handle);
    }

    private static void handle(Context ctx) {
        if (ctx.req.getParameter("key") == null) {
            ctx.status(400);
            log.error("No key supplied!");
            return;
        }

        UUID key;

        try {
            key = UUID.fromString(ctx.req.getParameter("key"));
        } catch (IllegalArgumentException e) {
            log.error("Key '{}' is malformed!", ctx.req.getParameter("key"));
            ctx.status(400);
            return;
        }

        Session s = DatabaseUtil.getSessionFactory().openSession();
        CriteriaBuilder cb = s.getCriteriaBuilder();

        CriteriaQuery<VerificationEntity> q = cb.createQuery(VerificationEntity.class);
        Root<VerificationEntity> r = q.from(VerificationEntity.class);
        List<VerificationEntity> entities = s.createQuery(q.select(r).where(
            cb.equal(r.get("key"), key))).getResultList();

        if (entities.isEmpty()) {
            log.error("Key {} not in database!", key);
            ctx.status(400);
            return;
        }

        VerificationEntity entity = entities.get(0);
        Gatekeeper.verify(entity);
        log.debug("User '{}' with key '{}' successfully verified on '{}'",
            entity.getUser_id(), entity.getKey(), entity.getGuild_id());
        ctx.status(201);
    }
}
