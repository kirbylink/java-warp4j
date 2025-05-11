package de.dddns.kirbylink.warp4j.model;

import java.util.List;
import de.dddns.kirbylink.warp4j.model.adoptium.v3.VersionData;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReleaseVersionsResponse {
  private List<VersionData> versions;
}
