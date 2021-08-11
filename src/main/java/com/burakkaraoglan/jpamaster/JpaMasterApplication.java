package com.burakkaraoglan.jpamaster;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
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
@EnableCaching
public class JpaMasterApplication {
    public static void main(String[] args) {
        SpringApplication.run(JpaMasterApplication.class, args);
    }

    @Bean
    @Transactional
    CommandLineRunner commandLineRunner(PersonRepository personRepository) {
        return args -> {
            Person person = Person.builder()
                    .name("Ahmet")
                    .build();
            person.getAddresses().addAll(Arrays.asList(
                    Address.builder()
                            .street("ahmetFirstStreet")
                            .person(person)
                            .build()));
            personRepository.save(person);
        };
    }
}

@RestController
@RequestMapping("/people")
@RequiredArgsConstructor
class PersonController {
    private final PersonService personService;

    @GetMapping
    @Cacheable("people")
    public List<PersonDto> getAllPeople() {
        return personService.findAllPeople();
    }
}

@Service
@RequiredArgsConstructor
@Slf4j
class PersonService {
    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final AddressRepository addressRepository;

    @Transactional(readOnly = true)
    public List<PersonDto> findAllPeople() {
        log.warn("1st {}", personMapper.personToPersonDto(personRepository.findAllPeople("Burak").get(0)));
        log.warn("2sc {}", personRepository.findAll());
        log.warn("3rd {}", personRepository.findAll());
        log.warn("4 {}", personRepository.findById(1L));
        log.warn("5 {}", personRepository.findById(4L));

        return personMapper.personListToPersonDtoList(personRepository.findAll());
    }
}

@Setter
@Getter
@ToString
class PersonDto {
    @NotBlank
    @JsonProperty("name")
    private String name;
    private Set<AddressDto> addresses;
}

@Setter
@Getter
@ToString
class AddressDto {
    @NotBlank
    private String street;
}

@Mapper
interface AddressMapper {
    AddressDto addressToAddressDto(Address address);
}

@Mapper// (componentModel = "spring", uses = {AddressMapper.class})
interface PersonMapper {

    PersonDto personToPersonDto(Person person);

    List<PersonDto> personListToPersonDtoList(List<Person> person);

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

        //log.warn("{}", personRepository.findAll());
        //log.debug("{}", personRepository.findAllPeople("Burak"));
        //log.debug("{}", addressRepository.findAll());
    }
}

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
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
@Getter
@Setter
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
