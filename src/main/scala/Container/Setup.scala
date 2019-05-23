package Container

import squants.time._

case class Setup(directoryPath: String,
                 networkService: NetworkService,
                 timeout: Time)
