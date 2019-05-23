package container

import container.Registry._
import org.apache.http.HttpHost

import scala.sys.process._

//import better.files.{File => BFile, _}
import better.files._
import container.Status._

import ImageBuilder._
import ContainerExecutor._
import ImageDownloader._

object Main extends App {

/*
  args match {
    case Array(name, tag) => buildImage(getImageFiles(DockerImage(name, tag)).get)
    case Array(name) => buildImage(getImageFiles(DockerImage(name)).get)
    case _ => println("\nMissing argument! Which Docker Image do you want?")
  }

*/
/*  def getImageFiles(dockerImage: DockerImage): Option[File] = {
    val opt: Option[HttpHost] = None
    val net = new NetworkService(opt)
    downloadLayers(dockerImage)(net)
  }

  def buildImage(source: File): Option[File] = {
    buildImage(source) match  {
      case OK => Some(source)
      case _ => None
    }
  }*/
/*
  def executeContainer(image : File): Unit = {

  }
*/
/*  println("downLoadImage")
  val testDSave = downloadImageWithDocker(DockerImage(args(0)))
  println("executeImage")

  executeContainerWithDocker(testDSave)
*/


//WORKIN WELL
 /*if (args.length > 2) {
  val command: String = args.drop(1).mkString(" ")

  val commandSeq = args.drop(1).toSeq
  //for (c <- commandSeq) println(c)

  val test = downloadContainerImage(DockerImage(args(0)))//, command = commandSeq))//, true)
  //val buildTest = buildImageFromTar(test)
  buildImageForDocker(test)

  // executeContainerWithDocker(test)
 }*/

 //else

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.util.{Failure,Success}
/*
  val test = ImageDownloader.Executor.sequential(downloadContainerImage(DockerImage(args(0))))
  test.onComplete{
    case Success(image) => println(s"Success! $image") ; ContainerExecutor.executeContainerWithDocker(image)
    case Failure(e) => println(s"Failure! $e")
  }
*/
  //ContainerExecutor.executeContainerWithDocker(buildImageForDocker(downloadContainerImage(DockerImage(args(0), command = Seq("/bin/ls")))))
   //ContainerExecutor.executeContainerWithDocker(buildImageForDocker(SavedDockerImage("python", File("/home/iscpif/containers/python").toJava, false, Seq("/bin/ls"))))//File("/home/iscpif/containers/p.tar").toJava)))// Seq("/bin/echo \"Hello world\"")))

 // buildImageForDocker(test)

 // Cache.collectImageData(DockerImage(args(0), args(1)))

/*
import io.circe._
import io.circe.parser._
*/

//  val correct = "{\"architecture\":\"amd64\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"python3\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"container\":\"a38d97f781f1b56394dc0c90bddab535137f71e6945426c0c93c0190c46ffabb\",\"container_config\":{\"Hostname\":\"a38d97f781f1\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) \",\"CMD [\\\"python3\\\"]\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":{}},\"created\":\"2019-02-12T21:37:33.563084814Z\",\"docker_version\":\"18.06.1-ce\",\"history\":[{\"created\":\"2019-02-06T03:30:01.714540068Z\",\"created_by\":\"/bin/sh -c #(nop) ADD file:4fec879fdca802d6920b8981b409b19ded75aff693eaaba1ba4cf5ecb7594fdb in / \"},{\"created\":\"2019-02-06T03:30:02.095682729Z\",\"created_by\":\"/bin/sh -c #(nop)  CMD [\\\"bash\\\"]\",\"empty_layer\":true},{\"created\":\"2019-02-06T05:51:03.607050513Z\",\"created_by\":\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tca-certificates \\t\\tcurl \\t\\tnetbase \\t\\twget \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"},{\"created\":\"2019-02-06T05:51:14.391936075Z\",\"created_by\":\"/bin/sh -c set -ex; \\tif ! command -v gpg \\u003e /dev/null; then \\t\\tapt-get update; \\t\\tapt-get install -y --no-install-recommends \\t\\t\\tgnupg \\t\\t\\tdirmngr \\t\\t; \\t\\trm -rf /var/lib/apt/lists/*; \\tfi\"},{\"created\":\"2019-02-06T05:51:50.706559429Z\",\"created_by\":\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tbzr \\t\\tgit \\t\\tmercurial \\t\\topenssh-client \\t\\tsubversion \\t\\t\\t\\tprocps \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"},{\"created\":\"2019-02-06T05:53:42.363280507Z\",\"created_by\":\"/bin/sh -c set -ex; \\tapt-get update; \\tapt-get install -y --no-install-recommends \\t\\tautoconf \\t\\tautomake \\t\\tbzip2 \\t\\tdpkg-dev \\t\\tfile \\t\\tg++ \\t\\tgcc \\t\\timagemagick \\t\\tlibbz2-dev \\t\\tlibc6-dev \\t\\tlibcurl4-openssl-dev \\t\\tlibdb-dev \\t\\tlibevent-dev \\t\\tlibffi-dev \\t\\tlibgdbm-dev \\t\\tlibgeoip-dev \\t\\tlibglib2.0-dev \\t\\tlibjpeg-dev \\t\\tlibkrb5-dev \\t\\tliblzma-dev \\t\\tlibmagickcore-dev \\t\\tlibmagickwand-dev \\t\\tlibncurses5-dev \\t\\tlibncursesw5-dev \\t\\tlibpng-dev \\t\\tlibpq-dev \\t\\tlibreadline-dev \\t\\tlibsqlite3-dev \\t\\tlibssl-dev \\t\\tlibtool \\t\\tlibwebp-dev \\t\\tlibxml2-dev \\t\\tlibxslt-dev \\t\\tlibyaml-dev \\t\\tmake \\t\\tpatch \\t\\tunzip \\t\\txz-utils \\t\\tzlib1g-dev \\t\\t\\t\\t$( \\t\\t\\tif apt-cache show 'default-libmysqlclient-dev' 2\\u003e/dev/null | grep -q '^Version:'; then \\t\\t\\t\\techo 'default-libmysqlclient-dev'; \\t\\t\\telse \\t\\t\\t\\techo 'libmysqlclient-dev'; \\t\\t\\tfi \\t\\t) \\t; \\trm -rf /var/lib/apt/lists/*\"},{\"created\":\"2019-02-06T12:02:34.387238501Z\",\"created_by\":\"/bin/sh -c #(nop)  ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"empty_layer\":true},{\"created\":\"2019-02-06T12:02:34.575599343Z\",\"created_by\":\"/bin/sh -c #(nop)  ENV LANG=C.UTF-8\",\"empty_layer\":true},{\"created\":\"2019-02-06T12:06:04.928405067Z\",\"created_by\":\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\ttk-dev \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"},{\"created\":\"2019-02-06T12:06:05.130875995Z\",\"created_by\":\"/bin/sh -c #(nop)  ENV GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"empty_layer\":true},{\"created\":\"2019-02-06T12:06:05.326758955Z\",\"created_by\":\"/bin/sh -c #(nop)  ENV PYTHON_VERSION=3.6.8\",\"empty_layer\":true},{\"created\":\"2019-02-06T12:08:21.018164802Z\",\"created_by\":\"/bin/sh -c set -ex \\t\\t\\u0026\\u0026 wget -O python.tar.xz \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz\\\" \\t\\u0026\\u0026 wget -O python.tar.xz.asc \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz.asc\\\" \\t\\u0026\\u0026 export GNUPGHOME=\\\"$(mktemp -d)\\\" \\t\\u0026\\u0026 gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys \\\"$GPG_KEY\\\" \\t\\u0026\\u0026 gpg --batch --verify python.tar.xz.asc python.tar.xz \\t\\u0026\\u0026 { command -v gpgconf \\u003e /dev/null \\u0026\\u0026 gpgconf --kill all || :; } \\t\\u0026\\u0026 rm -rf \\\"$GNUPGHOME\\\" python.tar.xz.asc \\t\\u0026\\u0026 mkdir -p /usr/src/python \\t\\u0026\\u0026 tar -xJC /usr/src/python --strip-components=1 -f python.tar.xz \\t\\u0026\\u0026 rm python.tar.xz \\t\\t\\u0026\\u0026 cd /usr/src/python \\t\\u0026\\u0026 gnuArch=\\\"$(dpkg-architecture --query DEB_BUILD_GNU_TYPE)\\\" \\t\\u0026\\u0026 ./configure \\t\\t--build=\\\"$gnuArch\\\" \\t\\t--enable-loadable-sqlite-extensions \\t\\t--enable-shared \\t\\t--with-system-expat \\t\\t--with-system-ffi \\t\\t--without-ensurepip \\t\\u0026\\u0026 make -j \\\"$(nproc)\\\" \\t\\u0026\\u0026 make install \\t\\u0026\\u0026 ldconfig \\t\\t\\u0026\\u0026 find /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' + \\t\\u0026\\u0026 rm -rf /usr/src/python \\t\\t\\u0026\\u0026 python3 --version\"},{\"created\":\"2019-02-06T12:08:21.831588152Z\",\"created_by\":\"/bin/sh -c cd /usr/local/bin \\t\\u0026\\u0026 ln -s idle3 idle \\t\\u0026\\u0026 ln -s pydoc3 pydoc \\t\\u0026\\u0026 ln -s python3 python \\t\\u0026\\u0026 ln -s python3-config python-config\"},{\"created\":\"2019-02-12T21:37:27.203581684Z\",\"created_by\":\"/bin/sh -c #(nop)  ENV PYTHON_PIP_VERSION=19.0.2\",\"empty_layer\":true},{\"created\":\"2019-02-12T21:37:33.384259286Z\",\"created_by\":\"/bin/sh -c set -ex; \\t\\twget -O get-pip.py 'https://bootstrap.pypa.io/get-pip.py'; \\t\\tpython get-pip.py \\t\\t--disable-pip-version-check \\t\\t--no-cache-dir \\t\\t\\\"pip==$PYTHON_PIP_VERSION\\\" \\t; \\tpip --version; \\t\\tfind /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' +; \\trm -f get-pip.py\"},{\"created\":\"2019-02-12T21:37:33.563084814Z\",\"created_by\":\"/bin/sh -c #(nop)  CMD [\\\"python3\\\"]\",\"empty_layer\":true}],\"os\":\"linux\",\"rootfs\":{\"type\":\"layers\",\"diff_ids\":[\"sha256:13d5529fd232cacdd8cd561148560e0bf5d65dbc1149faf0c68240985607c303\",\"sha256:abc3250a6c7ff22a6a366d9c175033ef0b2859f9d03676410c2f21d0fe568da4\",\"sha256:578414b395b98d02c5f284e83c8db080afcbbde8012478054af22df2edb9336d\",\"sha256:6257fa9f9597f43a167036d7bf748c5872c38803fa09e2493e119e512c6ac949\",\"sha256:a22a5ac18042c7ea8f9f4cb6f15bf5dda84a752fa8893a5a3d842b38285fba50\",\"sha256:14c77983a1cf162f827d258f346ac35ec48a3942334765a62a54d9198d651d04\",\"sha256:80b43ad4adf99bf0b0289ed39ef57c7ee4f486789dc62d962ca4cbb1d3c7c499\",\"sha256:f50f856f49faca830c3f7a64569bcce042a40c7c7efdf075cf88ef50754c8fe7\",\"sha256:195394b646efd1dcf12de7053a6e684ea109d1e8c864505da8d142e37b08c870\"]}}"
//  val correct_json = parse(correct).getOrElse(Json.Null)
//  println("GOOD JSON =" + correct_json + "\n\n\n")

  //  val reg = "{{\"architecture\":\"amd64\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"python3\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"container\":\"a38d97f781f1b56394dc0c90bddab535137f71e6945426c0c93c0190c46ffabb\",\"container_config\":{\"Hostname\":\"a38d97f781f1\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) \",\"CMD [\\\"python3\\\"]\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":{}},\"created\":\"2019-02-12T21:37:33.563084814Z\",\"docker_version\":\"18.06.1-ce\",\"id\":\"8bc0297f20768ebb7f753b5c9686fe425848d50bce9152f5e02354e6a5470c02\",\"os\":\"linux\",\"parent\":\"388086fb09673a409174363a7c81cf44430a1eed24d604b958e78ebb2c1f7149\",\"throwaway\":true},\n{\"id\":\"388086fb09673a409174363a7c81cf44430a1eed24d604b958e78ebb2c1f7149\",\"parent\":\"3993cade7c50f1b8d9928c353b795599b602d602b6d1b4360db191263da8283e\",\"created\":\"2019-02-12T21:37:33.384259286Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\t\\twget -O get-pip.py 'https://bootstrap.pypa.io/get-pip.py'; \\t\\tpython get-pip.py \\t\\t--disable-pip-version-check \\t\\t--no-cache-dir \\t\\t\\\"pip==$PYTHON_PIP_VERSION\\\" \\t; \\tpip --version; \\t\\tfind /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' +; \\trm -f get-pip.py\"]}},\n{\"id\":\"3993cade7c50f1b8d9928c353b795599b602d602b6d1b4360db191263da8283e\",\"parent\":\"4806dfce547d92d594398ed151ab2dc3ba49b339d3667ef16ce71bd1c88faf83\",\"created\":\"2019-02-12T21:37:27.203581684Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PYTHON_PIP_VERSION=19.0.2\"]},\"throwaway\":true},\n{\"id\":\"4806dfce547d92d594398ed151ab2dc3ba49b339d3667ef16ce71bd1c88faf83\",\"parent\":\"c133314d48162e20941d7f164f8ad0d63f7fd0b2f0a1bc853e578009936ad978\",\"created\":\"2019-02-06T12:08:21.831588152Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c cd /usr/local/bin \\t\\u0026\\u0026 ln -s idle3 idle \\t\\u0026\\u0026 ln -s pydoc3 pydoc \\t\\u0026\\u0026 ln -s python3 python \\t\\u0026\\u0026 ln -s python3-config python-config\"]}},\n{\"id\":\"c133314d48162e20941d7f164f8ad0d63f7fd0b2f0a1bc853e578009936ad978\",\"parent\":\"80a069f56efb96d66d5bb58033723a3c2817ad2b04beb496f410cfdef1423ffb\",\"created\":\"2019-02-06T12:08:21.018164802Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex \\t\\t\\u0026\\u0026 wget -O python.tar.xz \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz\\\" \\t\\u0026\\u0026 wget -O python.tar.xz.asc \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz.asc\\\" \\t\\u0026\\u0026 export GNUPGHOME=\\\"$(mktemp -d)\\\" \\t\\u0026\\u0026 gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys \\\"$GPG_KEY\\\" \\t\\u0026\\u0026 gpg --batch --verify python.tar.xz.asc python.tar.xz \\t\\u0026\\u0026 { command -v gpgconf \\u003e /dev/null \\u0026\\u0026 gpgconf --kill all || :; } \\t\\u0026\\u0026 rm -rf \\\"$GNUPGHOME\\\" python.tar.xz.asc \\t\\u0026\\u0026 mkdir -p /usr/src/python \\t\\u0026\\u0026 tar -xJC /usr/src/python --strip-components=1 -f python.tar.xz \\t\\u0026\\u0026 rm python.tar.xz \\t\\t\\u0026\\u0026 cd /usr/src/python \\t\\u0026\\u0026 gnuArch=\\\"$(dpkg-architecture --query DEB_BUILD_GNU_TYPE)\\\" \\t\\u0026\\u0026 ./configure \\t\\t--build=\\\"$gnuArch\\\" \\t\\t--enable-loadable-sqlite-extensions \\t\\t--enable-shared \\t\\t--with-system-expat \\t\\t--with-system-ffi \\t\\t--without-ensurepip \\t\\u0026\\u0026 make -j \\\"$(nproc)\\\" \\t\\u0026\\u0026 make install \\t\\u0026\\u0026 ldconfig \\t\\t\\u0026\\u0026 find /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' + \\t\\u0026\\u0026 rm -rf /usr/src/python \\t\\t\\u0026\\u0026 python3 --version\"]}},\n{\"id\":\"80a069f56efb96d66d5bb58033723a3c2817ad2b04beb496f410cfdef1423ffb\",\"parent\":\"5de94b39617a500bc65cce989c1ad4fcbda4a9fd765e55ecb48d609762387e89\",\"created\":\"2019-02-06T12:06:05.326758955Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PYTHON_VERSION=3.6.8\"]},\"throwaway\":true},\n{\"id\":\"5de94b39617a500bc65cce989c1ad4fcbda4a9fd765e55ecb48d609762387e89\",\"parent\":\"81112c80587583b460a3a64a9b54f85094c1db867a868218e23915cb73026b47\",\"created\":\"2019-02-06T12:06:05.130875995Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\"]},\"throwaway\":true},\n{\"id\":\"81112c80587583b460a3a64a9b54f85094c1db867a868218e23915cb73026b47\",\"parent\":\"01e7816c3054dd2ded24d4dca2d595c8501d4fa9d692a01c38b2998b6206cb26\",\"created\":\"2019-02-06T12:06:04.928405067Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\ttk-dev \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]}},\n{\"id\":\"01e7816c3054dd2ded24d4dca2d595c8501d4fa9d692a01c38b2998b6206cb26\",\"parent\":\"67ed2c162d9e08533291c4de0caea1d60b0bed90f0b4e2fec4076149d3705b84\",\"created\":\"2019-02-06T12:02:34.575599343Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV LANG=C.UTF-8\"]},\"throwaway\":true},\n{\"id\":\"67ed2c162d9e08533291c4de0caea1d60b0bed90f0b4e2fec4076149d3705b84\",\"parent\":\"c15b69c7b0e6b83540dff04f6481fee5bf7d67087cfe651735fdab9fd8885be7\",\"created\":\"2019-02-06T12:02:34.387238501Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"]},\"throwaway\":true},\n{\"id\":\"c15b69c7b0e6b83540dff04f6481fee5bf7d67087cfe651735fdab9fd8885be7\",\"parent\":\"64298a350df236c8a47753c51f0a89ca4e6e33d3c00180b41e80f25179052e12\",\"created\":\"2019-02-06T05:53:42.363280507Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\tapt-get update; \\tapt-get install -y --no-install-recommends \\t\\tautoconf \\t\\tautomake \\t\\tbzip2 \\t\\tdpkg-dev \\t\\tfile \\t\\tg++ \\t\\tgcc \\t\\timagemagick \\t\\tlibbz2-dev \\t\\tlibc6-dev \\t\\tlibcurl4-openssl-dev \\t\\tlibdb-dev \\t\\tlibevent-dev \\t\\tlibffi-dev \\t\\tlibgdbm-dev \\t\\tlibgeoip-dev \\t\\tlibglib2.0-dev \\t\\tlibjpeg-dev \\t\\tlibkrb5-dev \\t\\tliblzma-dev \\t\\tlibmagickcore-dev \\t\\tlibmagickwand-dev \\t\\tlibncurses5-dev \\t\\tlibncursesw5-dev \\t\\tlibpng-dev \\t\\tlibpq-dev \\t\\tlibreadline-dev \\t\\tlibsqlite3-dev \\t\\tlibssl-dev \\t\\tlibtool \\t\\tlibwebp-dev \\t\\tlibxml2-dev \\t\\tlibxslt-dev \\t\\tlibyaml-dev \\t\\tmake \\t\\tpatch \\t\\tunzip \\t\\txz-utils \\t\\tzlib1g-dev \\t\\t\\t\\t$( \\t\\t\\tif apt-cache show 'default-libmysqlclient-dev' 2\\u003e/dev/null | grep -q '^Version:'; then \\t\\t\\t\\techo 'default-libmysqlclient-dev'; \\t\\t\\telse \\t\\t\\t\\techo 'libmysqlclient-dev'; \\t\\t\\tfi \\t\\t) \\t; \\trm -rf /var/lib/apt/lists/*\"]}},\n{\"id\":\"64298a350df236c8a47753c51f0a89ca4e6e33d3c00180b41e80f25179052e12\",\"parent\":\"2ff9cb7d0492c206c8d99f8e4c43f80f9e8b3ae4ffd685f60c23b0d10a80bc6c\",\"created\":\"2019-02-06T05:51:50.706559429Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tbzr \\t\\tgit \\t\\tmercurial \\t\\topenssh-client \\t\\tsubversion \\t\\t\\t\\tprocps \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]}},\n{\"id\":\"2ff9cb7d0492c206c8d99f8e4c43f80f9e8b3ae4ffd685f60c23b0d10a80bc6c\",\"parent\":\"47b38a7fac3231d2dc442d6ebd9cc604ccffa3f785383f6202a275e4012f0f99\",\"created\":\"2019-02-06T05:51:14.391936075Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\tif ! command -v gpg \\u003e /dev/null; then \\t\\tapt-get update; \\t\\tapt-get install -y --no-install-recommends \\t\\t\\tgnupg \\t\\t\\tdirmngr \\t\\t; \\t\\trm -rf /var/lib/apt/lists/*; \\tfi\"]}},\n{\"id\":\"47b38a7fac3231d2dc442d6ebd9cc604ccffa3f785383f6202a275e4012f0f99\",\"parent\":\"8a71d4786d3d25edf9acf9e6783a1e3db45dd267be3033c3d1970c0552f7a95f\",\"created\":\"2019-02-06T05:51:03.607050513Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tca-certificates \\t\\tcurl \\t\\tnetbase \\t\\twget \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]}},\n{\"id\":\"8a71d4786d3d25edf9acf9e6783a1e3db45dd267be3033c3d1970c0552f7a95f\",\"parent\":\"e8d1c98545ca7e1b15f039c134c6cbdfe9c470563e1c602f7dbb884727b94827\",\"created\":\"2019-02-06T03:30:02.095682729Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  CMD [\\\"bash\\\"]\"]},\"throwaway\":true},\n{\"id\":\"e8d1c98545ca7e1b15f039c134c6cbdfe9c470563e1c602f7dbb884727b94827\",\"created\":\"2019-02-06T03:30:01.714540068Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) ADD file:4fec879fdca802d6920b8981b409b19ded75aff693eaaba1ba4cf5ecb7594fdb in / \"]}}}"
//  val reg = "{\"architecture\":\"amd64\",\"config\":{\"Hostname\":\"\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"python3\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":null},\"container\":\"a38d97f781f1b56394dc0c90bddab535137f71e6945426c0c93c0190c46ffabb\",\"container_config\":{\"Hostname\":\"a38d97f781f1\",\"Domainname\":\"\",\"User\":\"\",\"AttachStdin\":false,\"AttachStdout\":false,\"AttachStderr\":false,\"Tty\":false,\"OpenStdin\":false,\"StdinOnce\":false,\"Env\":[\"PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\",\"LANG=C.UTF-8\",\"GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\",\"PYTHON_VERSION=3.6.8\",\"PYTHON_PIP_VERSION=19.0.2\"],\"Cmd\":[\"/bin/sh\",\"-c\",\"#(nop) \",\"CMD [\\\"python3\\\"]\"],\"ArgsEscaped\":true,\"Image\":\"sha256:b4146f74f254b035dc14f507dcec3f64cf0e09c8d0476de9e692275da660dc84\",\"Volumes\":null,\"WorkingDir\":\"\",\"Entrypoint\":null,\"OnBuild\":null,\"Labels\":{}},\"created\":\"2019-02-12T21:37:33.563084814Z\",\"docker_version\":\"18.06.1-ce\",\"id\":\"8bc0297f20768ebb7f753b5c9686fe425848d50bce9152f5e02354e6a5470c02\",\"os\":\"linux\",\"parent\":\"388086fb09673a409174363a7c81cf44430a1eed24d604b958e78ebb2c1f7149\",\"throwaway\":true,\n\"id\":\"388086fb09673a409174363a7c81cf44430a1eed24d604b958e78ebb2c1f7149\",\"parent\":\"3993cade7c50f1b8d9928c353b795599b602d602b6d1b4360db191263da8283e\",\"created\":\"2019-02-12T21:37:33.384259286Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\t\\twget -O get-pip.py 'https://bootstrap.pypa.io/get-pip.py'; \\t\\tpython get-pip.py \\t\\t--disable-pip-version-check \\t\\t--no-cache-dir \\t\\t\\\"pip==$PYTHON_PIP_VERSION\\\" \\t; \\tpip --version; \\t\\tfind /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' +; \\trm -f get-pip.py\"]},\n\"id\":\"3993cade7c50f1b8d9928c353b795599b602d602b6d1b4360db191263da8283e\",\"parent\":\"4806dfce547d92d594398ed151ab2dc3ba49b339d3667ef16ce71bd1c88faf83\",\"created\":\"2019-02-12T21:37:27.203581684Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PYTHON_PIP_VERSION=19.0.2\"]},\"throwaway\":true,\n\"id\":\"4806dfce547d92d594398ed151ab2dc3ba49b339d3667ef16ce71bd1c88faf83\",\"parent\":\"c133314d48162e20941d7f164f8ad0d63f7fd0b2f0a1bc853e578009936ad978\",\"created\":\"2019-02-06T12:08:21.831588152Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c cd /usr/local/bin \\t\\u0026\\u0026 ln -s idle3 idle \\t\\u0026\\u0026 ln -s pydoc3 pydoc \\t\\u0026\\u0026 ln -s python3 python \\t\\u0026\\u0026 ln -s python3-config python-config\"]},\n\"id\":\"c133314d48162e20941d7f164f8ad0d63f7fd0b2f0a1bc853e578009936ad978\",\"parent\":\"80a069f56efb96d66d5bb58033723a3c2817ad2b04beb496f410cfdef1423ffb\",\"created\":\"2019-02-06T12:08:21.018164802Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex \\t\\t\\u0026\\u0026 wget -O python.tar.xz \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz\\\" \\t\\u0026\\u0026 wget -O python.tar.xz.asc \\\"https://www.python.org/ftp/python/${PYTHON_VERSION%%[a-z]*}/Python-$PYTHON_VERSION.tar.xz.asc\\\" \\t\\u0026\\u0026 export GNUPGHOME=\\\"$(mktemp -d)\\\" \\t\\u0026\\u0026 gpg --batch --keyserver ha.pool.sks-keyservers.net --recv-keys \\\"$GPG_KEY\\\" \\t\\u0026\\u0026 gpg --batch --verify python.tar.xz.asc python.tar.xz \\t\\u0026\\u0026 { command -v gpgconf \\u003e /dev/null \\u0026\\u0026 gpgconf --kill all || :; } \\t\\u0026\\u0026 rm -rf \\\"$GNUPGHOME\\\" python.tar.xz.asc \\t\\u0026\\u0026 mkdir -p /usr/src/python \\t\\u0026\\u0026 tar -xJC /usr/src/python --strip-components=1 -f python.tar.xz \\t\\u0026\\u0026 rm python.tar.xz \\t\\t\\u0026\\u0026 cd /usr/src/python \\t\\u0026\\u0026 gnuArch=\\\"$(dpkg-architecture --query DEB_BUILD_GNU_TYPE)\\\" \\t\\u0026\\u0026 ./configure \\t\\t--build=\\\"$gnuArch\\\" \\t\\t--enable-loadable-sqlite-extensions \\t\\t--enable-shared \\t\\t--with-system-expat \\t\\t--with-system-ffi \\t\\t--without-ensurepip \\t\\u0026\\u0026 make -j \\\"$(nproc)\\\" \\t\\u0026\\u0026 make install \\t\\u0026\\u0026 ldconfig \\t\\t\\u0026\\u0026 find /usr/local -depth \\t\\t\\\\( \\t\\t\\t\\\\( -type d -a \\\\( -name test -o -name tests \\\\) \\\\) \\t\\t\\t-o \\t\\t\\t\\\\( -type f -a \\\\( -name '*.pyc' -o -name '*.pyo' \\\\) \\\\) \\t\\t\\\\) -exec rm -rf '{}' + \\t\\u0026\\u0026 rm -rf /usr/src/python \\t\\t\\u0026\\u0026 python3 --version\"]},\n\"id\":\"80a069f56efb96d66d5bb58033723a3c2817ad2b04beb496f410cfdef1423ffb\",\"parent\":\"5de94b39617a500bc65cce989c1ad4fcbda4a9fd765e55ecb48d609762387e89\",\"created\":\"2019-02-06T12:06:05.326758955Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PYTHON_VERSION=3.6.8\"]},\"throwaway\":true,\n\"id\":\"5de94b39617a500bc65cce989c1ad4fcbda4a9fd765e55ecb48d609762387e89\",\"parent\":\"81112c80587583b460a3a64a9b54f85094c1db867a868218e23915cb73026b47\",\"created\":\"2019-02-06T12:06:05.130875995Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV GPG_KEY=0D96DF4D4110E5C43FBFB17F2D347EA6AA65421D\"]},\"throwaway\":true,\n\"id\":\"81112c80587583b460a3a64a9b54f85094c1db867a868218e23915cb73026b47\",\"parent\":\"01e7816c3054dd2ded24d4dca2d595c8501d4fa9d692a01c38b2998b6206cb26\",\"created\":\"2019-02-06T12:06:04.928405067Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\ttk-dev \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]},\n\"id\":\"01e7816c3054dd2ded24d4dca2d595c8501d4fa9d692a01c38b2998b6206cb26\",\"parent\":\"67ed2c162d9e08533291c4de0caea1d60b0bed90f0b4e2fec4076149d3705b84\",\"created\":\"2019-02-06T12:02:34.575599343Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV LANG=C.UTF-8\"]},\"throwaway\":true,\n\"id\":\"67ed2c162d9e08533291c4de0caea1d60b0bed90f0b4e2fec4076149d3705b84\",\"parent\":\"c15b69c7b0e6b83540dff04f6481fee5bf7d67087cfe651735fdab9fd8885be7\",\"created\":\"2019-02-06T12:02:34.387238501Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  ENV PATH=/usr/local/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin\"]},\"throwaway\":true,\n\"id\":\"c15b69c7b0e6b83540dff04f6481fee5bf7d67087cfe651735fdab9fd8885be7\",\"parent\":\"64298a350df236c8a47753c51f0a89ca4e6e33d3c00180b41e80f25179052e12\",\"created\":\"2019-02-06T05:53:42.363280507Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\tapt-get update; \\tapt-get install -y --no-install-recommends \\t\\tautoconf \\t\\tautomake \\t\\tbzip2 \\t\\tdpkg-dev \\t\\tfile \\t\\tg++ \\t\\tgcc \\t\\timagemagick \\t\\tlibbz2-dev \\t\\tlibc6-dev \\t\\tlibcurl4-openssl-dev \\t\\tlibdb-dev \\t\\tlibevent-dev \\t\\tlibffi-dev \\t\\tlibgdbm-dev \\t\\tlibgeoip-dev \\t\\tlibglib2.0-dev \\t\\tlibjpeg-dev \\t\\tlibkrb5-dev \\t\\tliblzma-dev \\t\\tlibmagickcore-dev \\t\\tlibmagickwand-dev \\t\\tlibncurses5-dev \\t\\tlibncursesw5-dev \\t\\tlibpng-dev \\t\\tlibpq-dev \\t\\tlibreadline-dev \\t\\tlibsqlite3-dev \\t\\tlibssl-dev \\t\\tlibtool \\t\\tlibwebp-dev \\t\\tlibxml2-dev \\t\\tlibxslt-dev \\t\\tlibyaml-dev \\t\\tmake \\t\\tpatch \\t\\tunzip \\t\\txz-utils \\t\\tzlib1g-dev \\t\\t\\t\\t$( \\t\\t\\tif apt-cache show 'default-libmysqlclient-dev' 2\\u003e/dev/null | grep -q '^Version:'; then \\t\\t\\t\\techo 'default-libmysqlclient-dev'; \\t\\t\\telse \\t\\t\\t\\techo 'libmysqlclient-dev'; \\t\\t\\tfi \\t\\t) \\t; \\trm -rf /var/lib/apt/lists/*\"]},\n\"id\":\"64298a350df236c8a47753c51f0a89ca4e6e33d3c00180b41e80f25179052e12\",\"parent\":\"2ff9cb7d0492c206c8d99f8e4c43f80f9e8b3ae4ffd685f60c23b0d10a80bc6c\",\"created\":\"2019-02-06T05:51:50.706559429Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tbzr \\t\\tgit \\t\\tmercurial \\t\\topenssh-client \\t\\tsubversion \\t\\t\\t\\tprocps \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]},\n\"id\":\"2ff9cb7d0492c206c8d99f8e4c43f80f9e8b3ae4ffd685f60c23b0d10a80bc6c\",\"parent\":\"47b38a7fac3231d2dc442d6ebd9cc604ccffa3f785383f6202a275e4012f0f99\",\"created\":\"2019-02-06T05:51:14.391936075Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c set -ex; \\tif ! command -v gpg \\u003e /dev/null; then \\t\\tapt-get update; \\t\\tapt-get install -y --no-install-recommends \\t\\t\\tgnupg \\t\\t\\tdirmngr \\t\\t; \\t\\trm -rf /var/lib/apt/lists/*; \\tfi\"]},\n\"id\":\"47b38a7fac3231d2dc442d6ebd9cc604ccffa3f785383f6202a275e4012f0f99\",\"parent\":\"8a71d4786d3d25edf9acf9e6783a1e3db45dd267be3033c3d1970c0552f7a95f\",\"created\":\"2019-02-06T05:51:03.607050513Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c apt-get update \\u0026\\u0026 apt-get install -y --no-install-recommends \\t\\tca-certificates \\t\\tcurl \\t\\tnetbase \\t\\twget \\t\\u0026\\u0026 rm -rf /var/lib/apt/lists/*\"]},\n\"id\":\"8a71d4786d3d25edf9acf9e6783a1e3db45dd267be3033c3d1970c0552f7a95f\",\"parent\":\"e8d1c98545ca7e1b15f039c134c6cbdfe9c470563e1c602f7dbb884727b94827\",\"created\":\"2019-02-06T03:30:02.095682729Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop)  CMD [\\\"bash\\\"]\"]},\"throwaway\":true,\n\"id\":\"e8d1c98545ca7e1b15f039c134c6cbdfe9c470563e1c602f7dbb884727b94827\",\"created\":\"2019-02-06T03:30:01.714540068Z\",\"container_config\":{\"Cmd\":[\"/bin/sh -c #(nop) ADD file:4fec879fdca802d6920b8981b409b19ded75aff693eaaba1ba4cf5ecb7594fdb in / \"]},\n\"rootfs\" : {\n    \"type\" : \"layers\",\n    \"diff_ids\" : [\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:aa45785265a066001ba5e566b722e55506ecc438f6c1d0d517b913dbff9f43b2\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:735a9c9a10bab4b523ec7a18d5935ac5fbd03dc8cc73ba954e99a1c0fe1de7f3\",\n\"sha256:99d78cb2765696e65ae9824c3b9663864b61f0be309e2157a4a1215524f0c4c9\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:36185429e34cebc40daa57f3435b09266c42a321d4ad06ce7baf0678f2d3a669\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:65c95cb8b3be70ecc403d2622ee4098d481d1b95c0e6ed70fa0582cf31a961d1\",\n\"sha256:7f0334c36886bd4619e8d05ccf68003e53b0b6098b2166a216bd009dba678ed8\",\n\"sha256:0a108aa2667933b852b8d003f97b344d014fcd7e06a0c0e6e04f2d6906738388\",\n\"sha256:34d8874714d74b636739b8a52204650a664fca8ff9741dd66810f30196f103e2\",\n\"sha256:a3ed95caeb02ffe68cdd9fd84406680ae93d633cb16422d00e8a7c22955b46d4\",\n\"sha256:741437d97401b83849ccbfe4ed8964049b752081647d7f1ea8aea29d989f8968\"\n    ]\n  }\n}"
//  val reg_json = parse(reg).getOrElse(Json.Null)
  //println("REG JSON =" + reg_json)


/*
val test = downloadImageForDocker(DockerImage(args(0)/*, args(1)*/)) //, true)
*/











 //  val buildTest = buildImage(test)

 //  executeContainerWithoutDocker(buildTest)


}
























/*

  case class DockerContainerConfig(Hostname: String = "",
                                   DomainName: String = "",
                                   User : String = "",
                                   AttachStdin: Boolean = false,
                                   AttachStdout: Boolean = false,
                                   AttachStderr: Boolean = false,
                                   Tty: Boolean = false,
                                   OpenStdin: Boolean = false,
                                   StdinOnce: Boolean = false,
                                   Env: List[String] = null,
                                   Cmd: List[String] = null,
                                   Image: String = "",
                                   Volumes: List[String] = null,
                                   workingDir: String = "",
                                   Entry)




  case class DockerConfig(id: String,
                          parent: String,
                          created: String,
                          container_config: ???,
                          os: String)

 */