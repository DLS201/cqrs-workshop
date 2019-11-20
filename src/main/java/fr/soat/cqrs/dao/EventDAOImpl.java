package fr.soat.cqrs.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class EventDAOImpl implements EventDAO {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public EventDAOImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public boolean exists(String eventHash) {
        //FIXME
        String sql = "SELECT EXISTS(SELECT 1 FROM consumed_event WHERE hash = ? LIMIT 1)";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, eventHash);
        return exists;
    }

    @Override
    public void insert(String eventHash) {
        //FIXME
        if (exists(eventHash)) {
            throw new DuplicateKeyException("consumed_event_pkey");
        }

        String sql = "INSERT INTO consumed_event(hash) VALUES (?)";
        jdbcTemplate.update(sql, eventHash);
    }
}
