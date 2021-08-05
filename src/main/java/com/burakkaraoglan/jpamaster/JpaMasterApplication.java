package com.burakkaraoglan.jpamaster;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SpringBootApplication
public class JpaMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(JpaMasterApplication.class, args);
    }

}

@RestController
@RequestMapping("/people")
@RequiredArgsConstructor
class PersonController {
    private final PersonService personService;

    @GetMapping
    public List<PersonDto> getAllPeople() {
        return personService.findAllPeople();
    }
}

@Service
@RequiredArgsConstructor
class PersonService {
    private final PersonRepository personRepository;
    private final PersonMapper personMapper;

    public List<PersonDto> findAllPeople() {
        return personMapper.personListToPersonDtoList(personRepository.findAll());
    }
}

@Mapper(componentModel = "spring")
interface PersonMapper {
    PersonDto personToPersonDto(Person person);

    List<PersonDto> personListToPersonDtoList(List<Person> person);

}

class PersonDto {
    @NotBlank
    private String name;
    private Set<AddressDto> addresses;
}

class AddressDto {
    @NotBlank
    private String street;
}

@Component
@RequiredArgsConstructor
@Slf4j
class CommandLineRunnerImpl implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final AddressRepository addressRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Person person = Person.builder()
                .name("Burak")
                .build();
        person.getAddresses().addAll(Arrays.asList(
                Address.builder()
                        .street("firstStreet")
                        .person(person)
                        .build(),
                Address.builder()
                        .person(person)
                        .street("scStreet")
                        .build()));
        personRepository.save(person);

        log.warn("{}", personRepository.findAll());
        log.debug("{}", personRepository.findAllPeople("Burak"));
        log.debug("{}", addressRepository.findAll());
    }
}

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "person")
    private final Set<Address> addresses = new HashSet<>();
}

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = "person")
class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String street;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "person_id")
    private Person person;
}

@Repository
interface PersonRepository extends JpaRepository<Person, Long>, PersonRepositoryCustom {

}

@Repository
interface AddressRepository extends JpaRepository<Address, Long> {

}

interface PersonRepositoryCustom {
    List<Person> findAllPeople(final String name);
}

@Repository
@RequiredArgsConstructor
class PersonRepositoryCustomImpl implements PersonRepositoryCustom {
    private final EntityManager em;

    @Override
    public List<Person> findAllPeople(final String name) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Person> cq = cb.createQuery(Person.class);

        Root<Person> person = cq.from(Person.class);
        List<Predicate> predicates = new ArrayList<>();

        if (StringUtils.hasText(name)) {
            predicates.add(cb.equal(person.get("name"), name));
        }
        cq.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(cq).getResultList();
    }
}
