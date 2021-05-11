package wooteco.subway.section.model;

import wooteco.subway.exception.NotFoundException;
import wooteco.subway.exception.SectionAdditionException;
import wooteco.subway.exception.SectionDeleteException;
import wooteco.subway.station.model.Station;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Sections {

    private final List<Section> sections;

    public Sections(List<Section> sections) {
        this.sections = sections;
    }

    public List<Station> sortedStations() {
        List<Station> stations = sections.stream()
                .map(Section::getUpStation)
                .collect(Collectors.toList());
        stations.add(sections.get(stations.size() - 1).getDownStation());
        return stations;
    }

    public void add(Section newSection) {
        Station newUpStation = newSection.getUpStation();
        Station newDownStation = newSection.getDownStation();
        validateConnectableSection(newUpStation, newDownStation);

        if (sortedStations().contains(newUpStation)) {
            addSectionIfMatchingUpStation(newSection, newUpStation);
            return;
        }
        addSectionIfMatchingDownStation(newSection, newDownStation);
    }

    private void addSectionIfMatchingUpStation(Section newSection, Station newUpStation) {
        if (hasSectionMatchingUpStation(newUpStation)) { // 상행 - 상행 겹침
            Section matchingSection = getMatchingSectionByUpStation(newUpStation);
            addAfterMatchingStationIfValidDistance(newSection, matchingSection);
            return;
        }
        // 맨 뒤에 구간 추가.
        sections.add(newSection);
    }

    private void addSectionIfMatchingDownStation(Section newSection, Station newDownStation) {
        if (hasSectionMatchingDownStation(newDownStation)) { // 하행 - 하행 겹침
            Section matchingSection = getMatchingSectionByDownStation(newDownStation);
            addBeforeMatingStationIfValidDistance(newSection, matchingSection);
            return;
        }
        // 맨 앞에 구간 추가.
        sections.add(0, newSection);
    }

    private void addAfterMatchingStationIfValidDistance(Section newSection, Section matchingSection) {
        if (matchingSection.getDistance() > newSection.getDistance()) {
            // 구간 추가, 거리 수정
            int index = sections.indexOf(matchingSection);
            sections.add(index, newSection);
            sections.set(index + 1, matchingSection.splitByUpStation(newSection));
            return;
        }
        throw new SectionAdditionException("추가하는 구간의 거리가 더 짧아야합니다.");
    }

    private void addBeforeMatingStationIfValidDistance(Section newSection, Section matchingSection) {
        if (matchingSection.getDistance() > newSection.getDistance()) {
            // 구간 추가, 거리 수정
            int index = sections.indexOf(matchingSection);
            sections.add(index + 1, newSection);
            sections.set(index, matchingSection.splitByDownStation(newSection));
            return;
        }
        throw new SectionAdditionException("추가하는 구간의 거리가 더 짧아야합니다.");
    }

    private Section getMatchingSectionByUpStation(Station newUpStation) {
        return sections.stream()
                .filter(section -> section.hasUpStation(newUpStation))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당하는 구간이 없습니다."));
    }

    private Section getMatchingSectionByDownStation(Station newDownStation) {
        return sections.stream()
                .filter(section -> section.hasDownStation(newDownStation))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당하는 구간이 없습니다."));
    }

    private boolean hasSectionMatchingUpStation(Station newUpStation) {
        return sections.stream()
                .anyMatch(section -> section.hasUpStation(newUpStation));
    }

    private boolean hasSectionMatchingDownStation(Station newDownStation) {
        return sections.stream()
                .anyMatch(section -> section.hasDownStation(newDownStation));
    }

    private void validateConnectableSection(Station newUpStation, Station newDownStation) {
        if (!isConnectable(newUpStation, newDownStation)) {
            throw new SectionAdditionException("추가될 수 없는 구간입니다.");
        }
    }

    private boolean isConnectable(Station newUpStation, Station newDownStation) {
        List<Station> stations = sortedStations();
        return (stations.contains(newUpStation) && !stations.contains(newDownStation)) ||
                (!stations.contains(newUpStation) && stations.contains(newDownStation));
    }

    public List<Section> sections() {
        return Collections.unmodifiableList(sections);
    }

    public void delete(Long stationId) {
        List<Section> matchingSections = sections.stream()
                .filter(section -> section.hasStationId(stationId))
                .collect(Collectors.toList());

        validateDeletingSection(matchingSections);
        deleteOrMergeSection(matchingSections);
    }

    private void validateDeletingSection(List<Section> matchingSections) {
        if (sections.size() <= 1) {
            throw new SectionDeleteException("노선 내 최소한 2개의 역이 존재해야 합니다.");
        }
        if (matchingSections.isEmpty()) {
            throw new NotFoundException("노선 내 존재하는 역이 없습니다.");
        }
    }

    private void deleteOrMergeSection(List<Section> matchingSections) {
        if (matchingSections.size() == 1) {
            sections.removeAll(matchingSections);
            return;
        }
        Section downStationMatchingSection = matchingSections.get(0);
        Section upStationMatchingSection = matchingSections.get(1);
        Section mergedSection = downStationMatchingSection.merge(upStationMatchingSection);
        int index = sections.indexOf(downStationMatchingSection);
        sections.set(index, mergedSection);
        sections.remove(index + 1);
    }

}
