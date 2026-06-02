# Open Liberty Diagnostic Summary Feature - Implementation Plan

## Project Overview

**Feature Name**: Intelligent Diagnostic Summary with PDF Reports and Email Delivery (Hybrid Approach)
**Start Date**: TBD
**Target Completion**: 48-58 weeks (phased approach)
**Project Lead**: TBD
**Technical Proposal**: [diagnostic-summary-technical-proposal.md](./diagnostic-summary-technical-proposal.md)

**Key Feature**: Hybrid trigger approach with immediate alerts for critical issues AND scheduled proactive health reports

## Implementation Phases

### Phase 1: Core Infrastructure and PDF Generation (Weeks 1-10)

**Goal**: Establish foundation with event detection and PDF report generation

#### Milestone 1.1: Project Setup and Dependencies (Weeks 1-2) ✅ COMPLETE

- [x] Create feature bundle structure: `com.ibm.ws.diagnostics.summary`
- [x] Set up Gradle build configuration
- [x] Add dependencies:
  - [x] Apache PDFBox library (2.0.29)
  - [x] JFreeChart for visualizations (1.5.4)
  - [x] Jakarta Mail API (2.1)
- [x] Create feature manifest: `diagnosticSummary-1.0.mf`
- [x] Set up FAT test infrastructure
- [x] Create initial documentation structure

**Deliverables**:

- Feature bundle skeleton
- Build configuration
- Test infrastructure

#### Milestone 1.2: Event Detection Framework (Weeks 3-5) ✅ COMPLETE

- [x] Implement `DiagnosticEventDetector` class
  - [x] Hook into FFDC system
  - [x] Monitor exception events
  - [x] Track health check status
  - [x] Detect OOM conditions
- [x] Implement `EventAggregator` class
  - [x] Sliding time window implementation
  - [x] Event deduplication logic
  - [x] Threshold detection
- [x] Implement `ExceptionSignatureAnalyzer` class
  - [x] Stack trace signature generation
  - [x] Exception pattern matching
  - [x] Frequency analysis
- [ ] Create unit tests for event detection

**Deliverables**:

- Event detection framework
- Unit tests (>80% coverage)

#### Milestone 1.3: Configuration and Data Model (Weeks 5-6) ✅ COMPLETE

- [x] Define XML schema for `<diagnosticSummary>` element
- [x] Implement configuration parser
- [x] Create data model classes:
  - [x] `DiagnosticReport`
  - [x] `IssueDetails`
  - [x] `TraceRecommendation`
  - [x] `RootCauseAnalysis`
- [x] Implement JSON serialization/deserialization
- [x] Create configuration validation logic

**Deliverables**:

- Configuration schema
- Data model classes
- Configuration parser

#### Milestone 1.4: PDF Report Generator (Weeks 7-10) 🔄 IN PROGRESS

- [x] Implement `PDFReportGenerator` class
  - [x] Multi-page layout engine
  - [x] Header/footer generation
  - [x] Table of contents
- [x] Implement `ReportFormatter` class
  - [x] Executive summary formatting
  - [x] Issue details formatting
  - [x] Stack trace syntax highlighting
  - [x] Configuration snippet formatting
- [x] Implement chart generation
  - [x] Severity distribution pie chart
  - [ ] Timeline chart for issue occurrences
  - [ ] Confidence meter visualization
- [x] Implement `TraceInstructionFormatter` class
  - [x] Format trace specifications
  - [x] Generate step-by-step instructions
  - [x] Create visual indicators for confidence levels
- [ ] Create comprehensive FAT tests for PDF generation
- [ ] Performance testing for PDF generation

**Deliverables**:

- PDF report generator
- Sample PDF reports
- FAT tests
- Performance benchmarks

**Phase 1 Exit Criteria**:

- ✅ Event detection working for exceptions and FFDC
- ✅ PDF reports generated successfully
- ✅ All unit tests passing
- ✅ FAT tests passing
- ✅ PDF generation time < 5 seconds
- ✅ Memory overhead < 50MB

---

### Phase 2: Email Delivery and Scheduled Analysis (Weeks 11-18)

**Goal**: Implement email delivery with SMTP support and scheduled proactive analysis

#### Milestone 2.1: Email Service Foundation (Weeks 11-13)

- [ ] Implement `EmailDeliveryService` class
  - [ ] JavaMail API integration
  - [ ] SMTP/SMTPS support
  - [ ] Authentication handling (plain, TLS, OAuth2)
  - [ ] Connection pooling
- [ ] Implement `SMTPConfigurationManager` class
  - [ ] Parse mail session configuration
  - [ ] Validate SMTP settings
  - [ ] Support Liberty's existing mail session config
- [ ] Implement retry logic
  - [ ] Exponential backoff
  - [ ] Maximum retry attempts
  - [ ] Dead letter queue for failed deliveries
- [ ] Create unit tests with mock SMTP server

**Deliverables**:

- Email delivery service
- SMTP configuration support
- Unit tests

#### Milestone 2.2: Email Template Engine (Weeks 13-15)

- [ ] Implement `EmailTemplateEngine` class
  - [ ] HTML email generation
  - [ ] Template variable substitution
  - [ ] Severity-based styling
  - [ ] Responsive design for mobile
- [ ] Create email templates
  - [ ] Standard notification template
  - [ ] Critical issue escalation template
  - [ ] Digest mode template
- [ ] Implement attachment handling
  - [ ] PDF attachment
  - [ ] Filename generation
  - [ ] Size validation
- [ ] Create FAT tests for email delivery

**Deliverables**:

- Email template engine
- HTML email templates
- FAT tests

#### Milestone 2.3: Integration and Testing (Week 16)

- [ ] Integrate PDF generator with email service
- [ ] Implement end-to-end workflow
  - [ ] Event detection → Analysis → PDF → Email
- [ ] Create integration tests
- [ ] Performance testing
  - [ ] Email delivery throughput
  - [ ] Concurrent report generation
- [ ] Security review
  - [ ] Credential protection
  - [ ] Email injection prevention
  - [ ] Rate limiting

**Deliverables**:

- End-to-end integration
- Integration tests
- Security review report

**Phase 2 Exit Criteria**:

- ✅ Email delivery working with SMTP/SMTPS
- ✅ PDF reports attached successfully
- ✅ HTML emails rendering correctly
- ✅ All tests passing
- ✅ Email delivery time < 10 seconds
- ✅ Delivery success rate > 99%

---

### Phase 3: Trace Recommendation Engine (Weeks 17-26) ✅ COMPLETE

**Goal**: Implement intelligent trace string recommendations

#### Milestone 3.1: Trace Mapping Database (Weeks 17-19) ✅ COMPLETE

- [x] Design trace recommendation database schema
- [x] Create exception pattern to trace mapping
  - [x] SSL/TLS issues → SSL traces
  - [x] Database issues → JDBC traces
  - [x] Transaction issues → Transaction traces
  - [x] Authentication issues → Security traces
  - [x] JAX-RS issues → REST traces
- [x] Implement pattern matching algorithm
- [x] Create confidence scoring system
- [x] Implement log volume estimation

**Deliverables**:

- Trace mapping database
- Pattern matching algorithm
- Confidence scoring system

#### Milestone 3.2: Trace Recommendation Engine (Weeks 20-23) ✅ COMPLETE

- [x] Implement `TraceRecommendationEngine` class
  - [x] Exception analysis
  - [x] Trace selection logic
  - [x] Confidence ranking
  - [x] Progressive strategy implementation
- [x] Implement `TracePackageMapper` class
  - [x] Map exceptions to Liberty packages
  - [x] Handle multiple trace specifications
  - [x] Prioritize traces by diagnostic value
- [x] Implement `LogVolumeEstimator` class
  - [x] Estimate log volume per trace
  - [x] Calculate total log size
  - [x] Provide performance impact warnings
- [ ] Create unit tests for trace recommendations

**Deliverables**:

- Trace recommendation engine
- Unit tests
- Trace mapping documentation

#### Milestone 3.3: L2 Support Package Generator (Weeks 24-26) ✅ COMPLETE

- [x] Implement `L2SupportPackageGenerator` class
  - [x] File collection checklist generation
  - [x] Sanitization instructions
  - [x] Packaging commands
  - [x] Support case template
- [x] Integrate with PDF report generator
  - [x] Add trace recommendations section
  - [x] Add L2 support guidance section
  - [x] Format trace instructions
- [x] Create comprehensive examples
- [ ] Update FAT tests

**Deliverables**:

- L2 support package generator
- Updated PDF reports with trace recommendations
- FAT tests

**Phase 3 Exit Criteria**:

- ✅ Trace recommendations generated for common issues
- ✅ Confidence scores accurate (>85% relevance)
- ✅ Log volume estimates within 20% of actual
- ✅ L2 support guidance complete and clear
- ✅ All tests passing

---

### Phase 4: Analysis Engine (Weeks 27-36)

**Goal**: Implement root cause analysis with confidence scoring

#### Milestone 4.1: Root Cause Analysis Framework (Weeks 27-30) ✅ COMPLETE

- [x] Implement `DiagnosticAnalyzer` class (RootCauseAnalyzer)
  - [x] Multi-symptom correlation
  - [x] Root cause inference
  - [x] Evidence collection
  - [x] Confidence calculation
- [x] Implement `ConfidenceScorer` class
  - [x] Scoring algorithm
  - [x] Evidence weighting
  - [x] Confidence threshold tuning
- [x] Create analysis rules engine (11 pre-configured patterns)
- [ ] Implement unit tests

**Deliverables**:

- Root cause analysis framework
- Confidence scoring system
- Unit tests

#### Milestone 4.2: Known Issues Database (Weeks 31-34)

- [ ] Design known issues database schema
- [ ] Implement `KnownIssuesMatcher` class
  - [ ] Pattern matching against known issues
  - [ ] Similarity scoring
  - [ ] Issue ranking
- [ ] Populate initial known issues database
  - [ ] Common SSL issues
  - [ ] Database connection issues
  - [ ] Transaction issues
  - [ ] Memory issues
- [ ] Implement APAR linking
  - [ ] APAR database integration
  - [ ] Relevance scoring
  - [ ] Link generation
- [ ] Create unit tests

**Deliverables**:

- Known issues database
- Issue matching engine
- APAR linking
- Unit tests

#### Milestone 4.3: Integration and Enhancement (Weeks 35-36)

- [ ] Integrate analysis engine with PDF generator
  - [ ] Add root cause analysis section
  - [ ] Add confidence visualizations
  - [ ] Add related issues section
- [ ] Enhance email notifications
  - [ ] Include top root cause in email
  - [ ] Add confidence indicators
- [ ] Create comprehensive FAT tests
- [ ] Performance optimization

**Deliverables**:

- Enhanced PDF reports with root cause analysis
- Updated email templates
- FAT tests

**Phase 4 Exit Criteria**:

- ✅ Root cause analysis working for common issues
- ✅ Confidence scores meaningful and accurate
- ✅ Known issues matching effective
- ✅ APAR links relevant
- ✅ All tests passing

---

### Phase 5: Advanced Triggers and Analysis (Weeks 37-46)

**Goal**: Implement advanced detection and analysis capabilities

#### Milestone 5.1: Health Check Integration (Weeks 37-39)

- [ ] Implement health check failure detection
  - [ ] Startup check monitoring
  - [ ] Liveness check monitoring
  - [ ] Readiness check monitoring
- [ ] Implement health check analysis
  - [ ] Failure pattern detection
  - [ ] Root cause correlation
  - [ ] Recovery recommendations
- [ ] Create FAT tests

**Deliverables**:

- Health check integration
- FAT tests

#### Milestone 5.2: Timeout and Circuit Breaker Analysis (Weeks 40-42)

- [ ] Implement MicroProfile Fault Tolerance integration
  - [ ] Timeout event detection
  - [ ] Circuit breaker state monitoring
  - [ ] Retry pattern analysis
- [ ] Implement pattern analysis
  - [ ] Identify timeout patterns
  - [ ] Correlate with other symptoms
  - [ ] Generate recommendations
- [ ] Create FAT tests

**Deliverables**:

- Fault tolerance integration
- Pattern analysis
- FAT tests

#### Milestone 5.3: OOM and Deadlock Detection (Weeks 43-45)

- [ ] Implement OOM detection
  - [ ] Monitor heap usage
  - [ ] Detect OutOfMemoryError
  - [ ] Analyze GC patterns
- [ ] Implement deadlock detection
  - [ ] ThreadMXBean integration
  - [ ] Periodic deadlock scanning
  - [ ] Thread dump generation
- [ ] Implement memory analysis
  - [ ] Heap dump recommendations
  - [ ] GC logging recommendations
  - [ ] Memory leak detection
- [ ] Create FAT tests

**Deliverables**:

- OOM detection
- Deadlock detection
- Memory analysis
- FAT tests

#### Milestone 5.4: Enhanced Visualizations (Week 46)

- [ ] Add timeline charts to PDF reports
- [ ] Add trend analysis charts
- [ ] Add resource usage graphs
- [ ] Enhance confidence visualizations

**Deliverables**:

- Enhanced PDF reports with visualizations

**Phase 5 Exit Criteria**:

- ✅ Health check failures detected and analyzed
- ✅ Timeout patterns identified
- ✅ OOM and deadlock detection working
- ✅ Enhanced visualizations in reports
- ✅ All tests passing

---

### Phase 6: Future Enhancements (Weeks 47-56) - OPTIONAL

**Goal**: Add REST API, UI, and integration capabilities

#### Milestone 6.1: REST API (Weeks 47-50)

- [ ] Design REST API endpoints
- [ ] Implement API bundle
- [ ] Add authentication/authorization
- [ ] Create API documentation
- [ ] Create FAT tests

**Deliverables**:

- REST API
- API documentation
- FAT tests

#### Milestone 6.2: Admin Center UI (Weeks 51-54)

- [ ] Design UI components
- [ ] Implement React components
- [ ] Add report viewing capability
- [ ] Add configuration UI
- [ ] Create UI tests

**Deliverables**:

- Admin Center UI components
- UI tests

#### Milestone 6.3: Event Integration (Weeks 55-56)

- [ ] Implement Kubernetes event generation
- [ ] Implement OpenTelemetry integration
- [ ] Implement webhook notifications
- [ ] Create integration tests

**Deliverables**:

- Event integrations
- Integration tests

---

### Phase 7: Testing and Documentation (Weeks 57-64)

**Goal**: Comprehensive testing, documentation, and release preparation

#### Milestone 7.1: Comprehensive Testing (Weeks 57-60)

- [ ] Complete FAT test coverage
  - [ ] All components tested
  - [ ] Edge cases covered
  - [ ] Error scenarios tested
- [ ] Performance testing
  - [ ] Load testing
  - [ ] Stress testing
  - [ ] Memory profiling
  - [ ] CPU profiling
- [ ] Security testing
  - [ ] Penetration testing
  - [ ] Vulnerability scanning
  - [ ] Code security review
- [ ] Compatibility testing
  - [ ] Different Java versions
  - [ ] Different OS platforms
  - [ ] Different SMTP servers

**Deliverables**:

- Complete test suite
- Performance test results
- Security test results
- Compatibility matrix

#### Milestone 7.2: Documentation (Weeks 61-63)

- [ ] User documentation
  - [ ] Feature overview
  - [ ] Configuration guide
  - [ ] Trace recommendation guide
  - [ ] L2 support package guide
  - [ ] Troubleshooting guide
- [ ] Developer documentation
  - [ ] Architecture documentation
  - [ ] API documentation
  - [ ] Extension guide
- [ ] Sample configurations
  - [ ] Basic configuration
  - [ ] Advanced configuration
  - [ ] Production configuration
- [ ] L2 support team training materials
  - [ ] Training presentation
  - [ ] Hands-on exercises
  - [ ] FAQ document

**Deliverables**:

- Complete documentation set
- Training materials

#### Milestone 7.3: Release Preparation (Week 64)

- [ ] Final code review
- [ ] Release notes preparation
- [ ] Migration guide (if needed)
- [ ] Known issues documentation
- [ ] Release candidate build
- [ ] Final testing
- [ ] Release approval

**Deliverables**:

- Release candidate
- Release notes
- Release approval

**Phase 7 Exit Criteria**:

- ✅ All tests passing (unit, FAT, integration, performance)
- ✅ Documentation complete and reviewed
- ✅ Security review passed
- ✅ Performance targets met
- ✅ Release approved

---

## Success Metrics

### Adoption Metrics

- [ ] Feature enablement rate > 30% within 6 months
- [ ] Report generation frequency tracked
- [ ] Email delivery success rate > 99%
- [ ] PDF report open/view rate > 80%

### Effectiveness Metrics

- [ ] Time to resolution improvement > 30%
- [ ] Support case reduction > 20%
- [ ] First-time-right diagnostic data submission > 80%
- [ ] User satisfaction score > 4.0/5.0
- [ ] False positive rate < 10%

### Performance Metrics

- [ ] Memory overhead < 50MB per server
- [ ] CPU overhead < 2% during normal operation
- [ ] PDF generation time < 5 seconds
- [ ] Email delivery time < 10 seconds
- [ ] Trace recommendation accuracy > 85%

---

## Risk Management

### Technical Risks

| Risk                           | Impact | Probability | Mitigation                               |
| ------------------------------ | ------ | ----------- | ---------------------------------------- |
| PDF library performance issues | High   | Medium      | Benchmark early, consider alternatives   |
| Email delivery failures        | High   | Medium      | Implement robust retry logic, monitoring |
| Trace recommendation accuracy  | Medium | Medium      | Extensive testing, feedback loop         |
| Memory overhead too high       | High   | Low         | Careful memory management, profiling     |
| Integration complexity         | Medium | Medium      | Phased approach, clear interfaces        |

### Schedule Risks

| Risk                  | Impact | Probability | Mitigation                            |
| --------------------- | ------ | ----------- | ------------------------------------- |
| Phase 1 delays        | High   | Medium      | Buffer time, prioritize core features |
| Dependency delays     | Medium | Low         | Early dependency identification       |
| Testing takes longer  | Medium | Medium      | Parallel testing, automated tests     |
| Resource availability | High   | Medium      | Cross-training, documentation         |

---

## Resource Requirements

### Development Team

- **Lead Developer**: 1 FTE (full project)
- **Developers**: 2-3 FTE (varies by phase)
- **QA Engineer**: 1 FTE (Phases 1-7)
- **Technical Writer**: 0.5 FTE (Phase 7)

### Infrastructure

- **Build servers**: Standard Liberty build infrastructure
- **Test servers**: 3-5 test servers for FAT tests
- **SMTP test server**: Mock SMTP server for testing
- **PDF generation testing**: Various OS platforms

---

## Communication Plan

### Weekly Status Updates

- Progress against milestones
- Blockers and risks
- Upcoming work

### Phase Reviews

- End of each phase review meeting
- Demo of completed functionality
- Lessons learned
- Adjustments for next phase

### Stakeholder Updates

- Monthly progress reports
- Quarterly executive summaries
- Release readiness reviews

---

## Next Steps

1. **Immediate Actions**:
   - [ ] Review and approve this implementation plan
   - [ ] Assign project lead and team members
   - [ ] Set up project infrastructure (repos, build, etc.)
   - [ ] Schedule Phase 1 kickoff meeting

2. **Week 1 Tasks**:
   - [ ] Create feature bundle structure
   - [ ] Set up Gradle build
   - [ ] Add initial dependencies
   - [ ] Create feature manifest
   - [ ] Set up FAT test infrastructure

3. **Ongoing**:
   - [ ] Weekly team meetings
   - [ ] Daily standups (if needed)
   - [ ] Continuous integration and testing
   - [ ] Regular stakeholder updates

---

## Appendix: Quick Reference

### Key Files and Locations

- **Feature Bundle**: `dev/com.ibm.ws.diagnostics.summary/`
- **FAT Tests**: `dev/com.ibm.ws.diagnostics.summary_fat/`
- **Feature Manifest**: `dev/com.ibm.ws.diagnostics.summary/resources/OSGI-INF/l10n/diagnosticSummary.properties`
- **Configuration Schema**: `dev/com.ibm.ws.diagnostics.summary/resources/OSGI-INF/metatype/metatype.xml`

### Important Commands

```bash
# Build feature
./gradlew com.ibm.ws.diagnostics.summary:build

# Run FAT tests
./gradlew com.ibm.ws.diagnostics.summary_fat:buildandrun

# Generate feature archive
./gradlew releaseNeeded
```

### Useful Links

- Technical Proposal: [diagnostic-summary-technical-proposal.md](./diagnostic-summary-technical-proposal.md)
- Liberty Build Guide: [CONTRIBUTING.md](../CONTRIBUTING.md)
- Liberty Feature Development: [Feature Development Guide](https://openliberty.io/docs/latest/reference/feature/feature-overview.html)
